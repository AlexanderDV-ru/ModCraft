package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.glClearColor;

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
import ru.alexanderdv.modcraft.interfaces.Shader;
import ru.alexanderdv.utils.ExceptionsHandler;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.VectorD;
import ru.alexanderdv.utils.MessageSystem.Msgs;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public class Main {
	public static void main(String args[]) { new ModCraft(String.join(" ", args)).start(); }

	public static class ModCraft implements VerticalNormalised, ToServerSender {
		Networking client, server;
		FramesManager frames;
		Textures textures;
		World world;
		UserController player;
		FirstPersonCamera firstPersonCamera;
		Camera camera/* current */;
		ArrayList<PhysicalPOV> physicals = new ArrayList<>();
		ArrayList<Shader> shaders = new ArrayList<>();
		String args;
		WorldConfig worldConfig;
		Time time;
		Commands commands;

//TODO realize a function how finded bug to moving world around you, not you around world, also interesting idea that worlds repeats, but with shift
		public ModCraft(String args) {
			Msgs.last = Msgs.msgs = new Msgs(this.args = args = this.getClass().getSimpleName() + " " + args) {};
			Config.defaults = new Config<String>(args.split(" "));
			Config.paths = new SConfig("configs/paths.cfg");

			frames = new FramesManager(args.split(" ")[0]);
			JFrame debugFrame = new JFrame("F3");
			debugFrame.setLayout(new java.awt.GridLayout(2, 1));
			JTextField console = new JTextField();
			console.setText("In game CONSOLE: write there command and press enter to perform");
			console.addActionListener((java.awt.event.ActionEvent arg0) -> { console.setText(commands.perform("console", console.getText())); });
			debugFrame.add(console);
			frames.debugFrame.init(debugFrame);
			frames.displayFrame.init(Config.hasArg("-displayInFrame", args) ? new JFrame() : null);
			frames.display.init(frames.displayFrame.frame);// TODO If rendering in other thread, it's also
			frames.input.init(frames.display);

			new SConfig("configs/textures.cfg").apply(textures = new Textures(), Block.names);
			Block.props = new SConfig("configs/blocks.cfg");
			world = (worldConfig = new WorldConfig("new_world.save")).getWorld();
			for (String name : args.split(" "))
				if (!name.startsWith("-") && name.length() > 0 && !name.equals(args.split(" ")[0])) {
					new PlayerConfig(player = new DamageableUserController(name)).configPlayer(world);
					player.input = frames.input;
					physicals.add(player);
					break;
				}
			camera = firstPersonCamera = new FirstPersonCamera();
			time = new Time();
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

		// TODO lava killing you, screen makes more red for full blindness
		// TODO healing block pumpkin or medicine
		// TODO inventory hotbar
		// TODO hearts in not spectator mode
		// TODO instructions - git wiki?

		public void start() {
			if (frames == null)
				return;
			ExceptionsHandler handler = new ExceptionsHandler();
			while (!isEnded())
				handler.tryCatchPrint(() -> {
					processAllControls();
					ticks();
					render();
					repaint();
					return null;
				});
			endProgram();
		}

		public void processAllControls() {
			for (PhysicalPOV physical : physicals) {
				physical.clearVelocityIncreasing(physical.velocity.coords, physical.velocityIncreasing.coords);
				physical.clearVelocityIncreasing(physical.volution.coords, physical.volutionIncreasing.coords);
			}
			frames.input.nextKeys.clear();
			frames.input.update();
			player.controls();
			performWaitingCommands();
			time.updateStopped = player.escape && !isClient() && !isServer();
		}

		public void ticks() {
			time.doRemainingUpdateCount(() -> {
				for (int i = 0; i < time.getPhysicalScale(); i++) {
					for (PhysicalPOV physical : physicals) {
						physical.physics(world);
						physical.applyVelocityIncreasing(physical.velocity.coords, physical.velocityIncreasing.coords, time.getTicksPerSecond());
						physical.applyVelocityIncreasing(physical.volution.coords, physical.volutionIncreasing.coords, time.getTicksPerSecond());
						physical.velocityMotionWithInertia(physical.position.coords, physical.velocity.coords, physical.getInertia(), world.collider);
						physical.velocityMotionWithInertia(physical.rotation.coords, physical.volution.coords, physical.getKinematics(), world.collider);
						Damageable damageable = physical instanceof Damageable ? ((Damageable) physical) : null;
						if (damageable != null) {
							damageable.damage(world.getBlock(physical.position).getDamage() / time.getTicksPerSecond());
							damageable.heal(damageable.getAutoHealing() / time.getTicksPerSecond());
						}
					}
					commands.command(player.getName(), player.selector(world, time.getTicksPerSecond(), false));
				}
			}, () -> {
				player.doForEachSeenBlock((x, y, z, w) -> world.calcNeedHide(x, y, z, w)/* fps x2 */);
				shaders.forEach((shader) -> shader.update());
			});
		}

		public void render() {
			camera.openEyes(frames.display.getWidth(), frames.display.getHeight(), player.vision.coords[0]);
			float blood = ((1 - (float) (((Damageable) player).getHealth() / ((Damageable) player).getMaxHealth()))) * (player.collision.disabled ? 0 : 1);
			if (player instanceof Damageable)
				glClearColor(blood, 0, 0, 1);
			shaders.forEach((shader) -> shader.render());
			camera.pointOfView(player);
			player.selectRenderDirectionByRotation(world);
			player.doForEachSeenBlock((x, y, z, w) -> {
				Block blockInCoords = world.getBlock(x, y, z, w);
				if (blockInCoords.id != 0) {
					glTranslated((int) x, (int) y, (int) z);
					if (blockInCoords.colorModifier == null)
						blockInCoords.colorModifier = new VectorD(4);
					blockInCoords.colorModifier.setX(blood);
					blockInCoords.colorModifier.setW(!player.transperantBlocksFromOtherWorlds ? 1 : 1 / (float) (0.5 + Math.abs(w - player.position.getW())));
					blockInCoords.render(world.isNeedHide(x, y, z, w));
					glTranslated(-(int) x, -(int) y, -(int) z);
				}
			});
			player.selector(world, time.getTicksPerSecond(), true);
			camera.closeEyes();
		}

		public void repaint() {
			frames.debugFrame.print(getF3());
			frames.displayFrame.print(getF3());
			frames.update();
		}

		private boolean isServer() { return !(args.split("-port:").length < 2 || args.split("-port:")[1].split(" ")[0].length() < 1); }

		private boolean isClient() { return !(args.split("-serverip:").length < 2 || args.split("-serverip:")[1].split(" ")[0].length() < 1); }

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
