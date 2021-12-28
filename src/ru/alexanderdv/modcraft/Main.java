package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JTextField;

import ru.alexanderdv.modcraft.Commands.ToServerSender;
import ru.alexanderdv.modcraft.Controller.UserController;
import ru.alexanderdv.modcraft.Input.Key;
import ru.alexanderdv.modcraft.configs.Config;
import ru.alexanderdv.modcraft.configs.PlayerConfig;
import ru.alexanderdv.modcraft.configs.SConfig;
import ru.alexanderdv.modcraft.configs.WorldConfig;
import ru.alexanderdv.modcraft.interfaces.Camera;
import ru.alexanderdv.modcraft.interfaces.Damageable;
import ru.alexanderdv.modcraft.interfaces.IWorld;
import ru.alexanderdv.modcraft.interfaces.Shader;
import ru.alexanderdv.utils.ExceptionsHandler;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.MessageSystem.Msgs;
import ru.alexanderdv.utils.VectorD;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public class Main {
	public static void main(String args[]) { new ModCraft(String.join(" ", args)).start(); }

	public static class ModCraft implements VerticalNormalised, ToServerSender {
		Networking client, server;
		FramesManager frames;
		Textures textures;
		IWorld world;
		UserController player;
		DefaultCamera defaultCamera;
		POV guiPOV;
		Camera camera/* current */;
		ArrayList<PhysicalPOV> physicals = new ArrayList<>();
		ArrayList<Shader> shaders = new ArrayList<>();
		String args;
		WorldConfig worldConfig;
		SConfig guiConfig;
		Time time;
		Commands commands;

//TODO realize a function how finded bug to moving world around you, not you around world, also interesting idea that worlds repeats, but with shift
		public ModCraft(String args) {
			Msgs.last = Msgs.msgs = new Msgs(this.args = args = this.getClass().getSimpleName() + " " + args) {};
			Config.defaults = new Config<String>(args.split(" "));
			Config.paths = new SConfig("configs/paths.cfg");
			guiConfig = new SConfig("configs/gui.cfg");

			createFrames(args);

			new SConfig("configs/textures.cfg").apply(textures = new Textures(), Block.names);
			Block.props = new SConfig("configs/blocks.cfg");
			world = (worldConfig = new WorldConfig("new_world.save")).getWorld();
			time = new Time();
			for (String name : args.split(" "))
				if (!name.startsWith("-") && name.length() > 0 && !name.equals(args.split(" ")[0])) {
					new PlayerConfig(player = new DamageableUserController(name)).configPlayer(world);
					player.input = frames.input;
					physicals.add(player);
					break;
				}
			camera = defaultCamera = new DefaultCamera();
			guiPOV = new POV();
			guiPOV.position.setZ(-1);
			commands = new Commands(this, player, new WorldEdit(world), time);

			final String ar = args;
			try {
				client = new Networking();
			} catch (Exception e) {}
			try {
				server = new Networking();
				server.startInExceptionThread(() -> server.startServer(player.getName(), MathUtils.parseI(ar.split("-port:")[1].split(" ")[0])), (e) -> {});
			} catch (Exception e) {}

			shaders.add(new FlickingShader());
		}

		public void createFrames(String args) {
			frames = new FramesManager(args.split(" ")[0]);
			JFrame debugFrame = new JFrame("F3");
			if (!guiConfig.bool("dontShowConsole"))
				debugFrame.setLayout(new java.awt.GridLayout(2, 1));
			JTextField console = new JTextField();
			console.setText("In game CONSOLE: write there command and press enter to perform");
			console.addActionListener((java.awt.event.ActionEvent arg0) -> { console.setText(commands.perform("console", console.getText())); });
			if (!guiConfig.bool("dontShowConsole"))
				debugFrame.add(console);
			frames.debugFrame.init(debugFrame);
			debugFrame.setVisible(!guiConfig.bool("dontShowDebugWindow"));
			frames.displayFrame.init(Config.hasArg("--displayInFrame", args) ? new JFrame() : null);
			frames.display.init(frames.displayFrame.frame);// TODO If rendering in other thread, it's also
			frames.display.setVSyncEnabled(Config.hasArg("--vsync", args));
			frames.display.setResizable(!Config.hasArg("--notresizable", args));
			try {
				frames.display.setFullscreen(Config.hasArg("--fullscreen", args));
			} catch (Exception e) {
				Msgs.last.debug(e);
			}
			frames.input.init(frames.display);
		}

		// TODO instructions - git wiki?
		// TODO make chunks for normal fps in big worlds

		public void start() {
			if (frames == null)
				return;
			ExceptionsHandler handler = new ExceptionsHandler();
			while (!isEnded())
				handler.tryCatchPrint(() -> {
					ticks();
					render();
					return null;
				});
			endProgram();
		}

		public void ticks() {
			time.doRemainingUpdateCount(() -> {
				for (PhysicalPOV physical : physicals) {
					physical.clearVelocityIncreasing(physical.velocity.coords, physical.velocityIncreasing.coords);
					physical.clearVelocityIncreasing(physical.volution.coords, physical.volutionIncreasing.coords);
				}
				frames.input.nextKeys.clear();
				frames.input.update();
				player.controls();
				performWaitingCommands();
				if (player.escape && !isClient() && !isServer())
					return;
				for (int i = 0; i < time.getPhysicalScale(); i++) {
					for (PhysicalPOV physical : physicals) {
						physical.physics(world);
						physical.applyVelocityIncreasing(physical.velocity.coords, physical.velocityIncreasing.coords, time.getTicksPerSecond());
						physical.applyVelocityIncreasing(physical.volution.coords, physical.volutionIncreasing.coords, time.getTicksPerSecond());
						physical.velocityMotionWithInertia(physical.position.coords, physical.velocity.coords, physical.getInertia(), world.getCollider());
						physical.velocityMotionWithInertia(physical.rotation.coords, physical.volution.coords, physical.getKinematics(), world.getCollider());
						Damageable damageable = physical instanceof Damageable ? ((Damageable) physical) : null;
						if (damageable != null) {
							damageable.damage(world.getBlock(physical.position).getDamage() / time.getTicksPerSecond());
							damageable.heal(damageable.getAutoHealing() / time.getTicksPerSecond());
						}
					}
					commands.command(player.getName(), player.selector(world, time.getTicksPerSecond(), false));
				}
			}, () -> shaders.forEach((shader) -> shader.update()));
		}

		public void render() {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glViewport(0, 0, (int) frames.display.getWidth(), (int) frames.display.getHeight());
			camera.openEyes(70, frames.display.getWidth(), frames.display.getHeight(), (float) player.vision.coords[0]);
			glEnable(GL_DEPTH_TEST);

			float blood = ((1 - (float) (((Damageable) player).getHealth() / ((Damageable) player).getMaxHealth()))) * (player.collision.disabled ? 0 : 1);
			if (player instanceof Damageable)
				glClearColor(blood, 0, 0, 1);
			shaders.forEach((shader) -> shader.render());
			camera.pointOfView(player);
			player.selectRenderDirectionByRotation(world);
			player.doForEachSeenBlock((x, y, z, w) -> {
				Block blockInCoords = world.getBlock(x, y, z, w);
				if (blockInCoords.getId() != 0) {
					glTranslated((int) x, (int) y, (int) z);
					if (blockInCoords.colorModifier == null)
						blockInCoords.colorModifier = new VectorD(4);
					blockInCoords.colorModifier.setX(blood);
					blockInCoords.colorModifier.setW(1 - (!player.transperantBlocksFromOtherWorlds ? 1 : 1 / (float) (0.5 + Math.abs(w - player.position.getW()))));
					blockInCoords.render(world.isNeedHide(x, y, z, w));
					glTranslated(-(int) x, -(int) y, -(int) z);
				}
			});
			player.selector(world, time.getTicksPerSecond(), true);
			camera.closeEyes();

			glDisable(GL_DEPTH_TEST);
			if (!guiConfig.bool("dontShowHotbar")) {
				for (int i = 0; i < player.inventory.length; i++) {
					double size = Math.min(frames.display.getWidth(), frames.display.getHeight()) / player.inventory.length;
					glViewport((int) (frames.display.getWidth() / 2 + -player.inventory.length * size / 2d + size * i), (int) size,

							(int) frames.display.getWidth() / player.inventory.length, (int) frames.display.getHeight() / player.inventory.length);
					camera.openEyes(70, (float) frames.display.getWidth(), (float) frames.display.getHeight(), 3);
					camera.pointOfView(guiPOV);
					glPushMatrix();
					glScaled(0.5, 0.5, 0.5);
					glTranslated(-0.7, -0.3, 0);
					glRotated(45, 1, 0, 0);
					glRotated(45, 0, 1, 0);
					new Block(0, 0, 0, 0, player.inventory[i]).render(new boolean[6]);
					if (i == player.selectedSlot)
						player.selector.render(new boolean[6]);
					glPopMatrix();
					camera.closeEyes();
				}
			}
			if (!guiConfig.bool("dontShowCrosshair")) {
				camera.openEyes(70, frames.display.getWidth(), frames.display.getHeight(), 1);
				camera.pointOfView(guiPOV);
				glViewport(0, 0, (int) frames.display.getWidth(), (int) frames.display.getHeight());
				cursor.render(frames.display.getWidth() / 2, frames.display.getHeight() / 2);
				camera.closeEyes();
			}
			frames.debugFrame.print(getF3());
			frames.displayFrame.print(getF3());
			frames.update();
		}

		Cursor cursor = Cursor.DoubleCrosshair;

		public static enum Cursor implements VerticalNormalised {
			DoubleCrosshair() {
				public void render(double width, double height) {
					glPushMatrix();
					glBindTexture(3553, 0);
					glScaled(1 / Math.min(width, height), 1 / Math.min(width, height), 1);

					glColor4f(0F, 0F, 0F, 1F);
					glBegin(GL_QUADS);
					glVertex3d(+2, -9, 0.0F);
					glVertex3d(-2, -9, 0.0F);
					glVertex3d(-2, +9, 0.0F);
					glVertex3d(+2, +9, 0.0F);

					glVertex3d(-9, -2, 0.0F);
					glVertex3d(-9, +2, 0.0F);
					glVertex3d(+9, +2, 0.0F);
					glVertex3d(+9, -2, 0.0F);
					glEnd();

					glColor4f(0.5F, 0.5F, 0.5F, 1F);
					glBegin(GL_QUADS);
					glVertex3d(+1, -8, 0.0F);
					glVertex3d(-1, -8, 0.0F);
					glVertex3d(-1, +8, 0.0F);
					glVertex3d(+1, +8, 0.0F);

					glVertex3d(-8, -1, 0.0F);
					glVertex3d(-8, +1, 0.0F);
					glVertex3d(+8, +1, 0.0F);
					glVertex3d(+8, -1, 0.0F);
					glEnd();

					glPopMatrix();
				}
			};

			public abstract void render(double screenWidth, double screenHeight);
		}

		public boolean isServer() { return !(args.split("-port:").length < 2 || args.split("-port:")[1].split(" ")[0].length() < 1); }

		public boolean isClient() { return !(args.split("-serverip:").length < 2 || args.split("-serverip:")[1].split(" ")[0].length() < 1); }

		@Override
		public void sendToServer(String executor, String cmd) {
			if (!isClient())
				return;
			client.writeRequests.put(cmd, "");
			client.startInExceptionThread(() -> client.startClient(executor, args.split("-serverip:")[1].split(" ")[0], MathUtils.parseI(args.split("-serverport:")[1].split(" ")[0])));
		}

		public void performWaitingCommands() {
			try {
				for (String cmd : server.readenRequests.keySet())
					commands.command(server.readenRequests.get(cmd), cmd);
				server.readenRequests.clear();
			} catch (Exception e) {}
			try {
				for (String cmd : client.readenRequests.keySet())
					commands.command(client.readenRequests.get(cmd), cmd);
				client.readenRequests.clear();
			} catch (Exception e) {}
			for (String macros : commands.commands.keySet())
				for (String macro : macros.split(","))
					if (macro.toLowerCase().startsWith("key_"))
						for (Key input : frames.input.nextKeys)
							if (input.isKeyDown(macro))
								commands.command("keyboard", macro);
		}

		private String getF3() { return "FPS: " + time.fps() + "\n" + player.getF3(); }

		private boolean isEnded() { return player != null && player.ended || frames != null && frames.isDestroyed(); }

		private void endProgram() {
			ExceptionsHandler.tryCatchVoid(() -> frames.destroy(), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> client.closeAll(), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> server.closeAll(), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> commands.close(), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> worldConfig.saveWorld(world), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> Msgs.last.debug("Closed with exit code 0"), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> System.exit(0), (e) -> e.printStackTrace());
		}
	}
}
