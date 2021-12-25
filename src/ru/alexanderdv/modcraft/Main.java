package ru.alexanderdv.modcraft;

import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JTextField;

import ru.alexanderdv.modcraft.Commands.ToServerSender;
import ru.alexanderdv.modcraft.Config.PlayerConfig;
import ru.alexanderdv.modcraft.Config.SConfig;
import ru.alexanderdv.modcraft.Config.WorldConfig;
import ru.alexanderdv.modcraft.Controller.UserController;
import ru.alexanderdv.modcraft.Input.Key;
import ru.alexanderdv.modcraft.Shader.FlickingShader;
import ru.alexanderdv.utils.ExceptionsHandler;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.MessageSystem.Msgs;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public class Main {
	public static void main(String args[]) { new ModCraft(String.join(" ", args)).start(); }

	public static class ModCraft implements VerticalNormalised, ToServerSender {
		public static class Player extends UserController implements Camera { public Player(String name) { super(name); } }

		Networking client, server;
		FramesManager frames;
		Textures textures;
		World world;
		Player player;
		ArrayList<PhysicalPOV> physicals = new ArrayList<>();
		ArrayList<Shader> shaders = new ArrayList<>();
		String args;
		WorldConfig worldConfig;
		Commands commands;

//TODO realize a function how finded bug to moving world around you, not you around world, also interesting idea that worlds repeats, but with shift
		public ModCraft(String args) {
			Msgs.last = Msgs.msgs = new Msgs(this.args = args = this.getClass().getSimpleName() + " " + args) {};
			Config.defaults = new Config<String>(args.split(" "));
			Config.paths = new SConfig("configs/paths.cfg");

			frames = new FramesManager(args.split(" ")[0]);
			frames.displayFrame.init(Config.hasArg("-displayInFrame", args) ? new JFrame() : null);
			frames.display.init(frames.displayFrame.frame);// TODO If rendering in other thread, it's also
			JFrame debugFrame = new JFrame("F3");
			debugFrame.setLayout(new java.awt.GridLayout(2, 1));
			JTextField console = new JTextField();
			console.setText("In game CONSOLE: write there command and press enter to perform");
			console.addActionListener((java.awt.event.ActionEvent arg0) -> { console.setText(commands.perform("console", console.getText())); });
			debugFrame.add(console);
			frames.debugFrame.init(debugFrame);
			frames.input.init(frames.display);

			new SConfig("configs/textures.cfg").apply(textures = new Textures(), Block.names);
			ExceptionsHandler.tryCatchVoid(() -> Msgs.last.debug("Loading world..."), (e) -> e.printStackTrace());
			world = (worldConfig = new WorldConfig("new_world.save")).getWorld();

			for (String name : args.split(" "))
				if (!name.startsWith("-") && name.length() > 0 && !name.equals(args.split(" ")[0])) {
					new PlayerConfig(player = new Player(name)).configPlayer(world);
					player.input = frames.input;
					physicals.add(player);
					break;
				}

			commands = new Commands(this, player, new WorldEdit(world));

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

		@Override
		public void sendToServer(String executor, String cmd) {
			if (args.split("-serverip:").length < 2 || args.split("-serverip:")[1].split(" ")[0].length() < 1)
				return;
			client.writeRequests.put(cmd, "");
			client.startInExceptionThread(() -> client.startClient(executor, args.split("-serverip:")[1].split(" ")[0], MathUtils.parseI(args.split("-serverport:")[1].split(" ")[0])));
		}

		Time time;

		public void start() {
			if (frames == null)
				return;
			time = new Time();
			ExceptionsHandler handler = new ExceptionsHandler();
			while (!stopped())
				handler.tryCatchPrint(() -> {
					for (PhysicalPOV physical : physicals) {
						physical.clearVelocityIncreasing(physical.velocity.coords, physical.velocityIncreasing.coords);
						physical.clearVelocityIncreasing(physical.volution.coords, physical.volutionIncreasing.coords);
					}

					frames.input.nextKeys.clear();
					frames.input.update();
					player.controls();
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
					time.changePhysicalScale(player.escape ? 0 : 1);

					player.openEyes(frames.display.getWidth(), frames.display.getHeight(), player.vision.coords[0]);

					shaders.forEach((shader) -> shader.render());
					player.pointOfView(player);
					player.selectRenderDirectionByRotation(world);
					player.doForEachSeenBlock((x, y, z, w) -> {
						Block blockInCoords = world.getBlock(x, y, z, w);
						if (blockInCoords.id != 0) {
							glTranslated((int) x, (int) y, (int) z);
							blockInCoords.opacity = !player.transperantBlocksFromOtherWorlds ? 1 : 1 / (float) (0.5 + Math.abs(w - player.position.getW()));
							blockInCoords.render(world.isNeedHide(x, y, z, w));
							glTranslated(-(int) x, -(int) y, -(int) z);
						}
					});
					player.selector(world, time.getTicksPerSecond(), true);
					time.doRemainingUpdateCount(() -> {
						for (int i = 0; i < time.getPhysicalScale(); i++) {
							for (PhysicalPOV physical : physicals) {
								physical.physics(world.enviroment);
								physical.applyVelocityIncreasing(physical.velocity.coords, physical.velocityIncreasing.coords, time.getTicksPerSecond());
								physical.applyVelocityIncreasing(physical.volution.coords, physical.volutionIncreasing.coords, time.getTicksPerSecond());
								physical.velocityMotionWithInertia(physical.position.coords, physical.velocity.coords, physical.getInertia(), world.collider);
								physical.velocityMotionWithInertia(physical.rotation.coords, physical.volution.coords, physical.getKinematics(), world.collider);
							}
							commands.command(player.getName(), player.selector(world, time.getTicksPerSecond(), false));
						}
					}, () -> {
						player.doForEachSeenBlock((x, y, z, w) -> world.calcNeedHide(x, y, z, w)/* fps x2 */);
						shaders.forEach((shader) -> shader.update());
					});
					player.closeEyes();
					frames.debugFrame.print(getF3());
					frames.displayFrame.print(getF3());
					frames.update();
					return null;
				});

			endProgram();
		}

		private String getF3() { return "FPS: " + time.fps() + "\n" + player.getF3(); }

		private boolean stopped() { return player != null && player.ended || frames != null && frames.isDestroyed(); }

		private void endProgram() {
			ExceptionsHandler.tryCatchVoid(() -> frames.destroy(), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> client.closeAll(), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> server.closeAll(), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> commands.close(), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> Msgs.last.debug("Saving world to '" + worldConfig.configuredPath + "'..."), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> worldConfig.saveWorld(world), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> Msgs.last.debug("Closed with exit code 0"), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> System.exit(0), (e) -> e.printStackTrace());
		}
	}
}
