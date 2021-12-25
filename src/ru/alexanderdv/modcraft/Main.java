package ru.alexanderdv.modcraft;

import java.util.ArrayList;

import javax.swing.JFrame;

import ru.alexanderdv.modcraft.Commands.ToServerSender;
import ru.alexanderdv.modcraft.Config.SConfig;
import ru.alexanderdv.modcraft.Controller.UserController;
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
		WorldManager worldManager;
		Commands commands;

		public ModCraft(String args) {
			Msgs.last = Msgs.msgs = new Msgs(this.args = args = this.getClass().getSimpleName() + " " + args) {};
			Config<String> defaults = Config.defaults = new Config<String>(args.split(" "));

			frames = new FramesManager(args.split(" ")[0]);
			frames.displayFrame.init(Config.hasArg("-displayInFrame", args) ? new JFrame() : null);
			frames.display.init(frames.displayFrame.frame);// TODO If rendering in other thread, it's also
			frames.debugFrame.init(new JFrame("F3"));
			frames.input.init(frames.display);

			new SConfig(defaults.configsS + "/" + defaults.texturesS + defaults.cfgExtS).apply(textures = new Textures(), Block.names);
			world = (worldManager = new WorldManager()).getWorld("world.txt");

			for (String name : args.split(" "))
				if (!name.startsWith("-") && name.length() > 0) {
					(player = new Player(name)).controls = new SConfig(defaults.playersS + "/" + name + "/" + defaults.controlsS + defaults.cfgExtS);
					player.input = frames.input;
					player.position.coords = new double[] { world.size[0] / 2, world.size[1] - 1, world.size[2] / 2 };
					player.rotation.coords[0] = 90;
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

		public void sendToServer(String executor, String cmd) {
			if (client.socket == null)
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
						physical.clearVelocityIncreasing(physical.velocity, physical.velocityIncreasing);
						physical.clearVelocityIncreasing(physical.volution, physical.volutionIncreasing);
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
					time.changePhysicalScale(player.escape ? 0 : 1);

					player.openEyes(frames.display.getWidth(), frames.display.getHeight(), 100d);

					shaders.forEach((shader) -> shader.render());
					player.pointOfView(player);
					player.selectRenderDirectionByRotation(world);
					player.doForEachSeenBlock((x, y, z) -> {
						if (world.getBlock(x, y, z).id != 0) {
							glTranslated(x, y, z);
							world.getBlock(x, y, z).render(world.isNeedHide(x, y, z));
							glTranslated(-x, -y, -z);
						}
					});
					player.selector(world, time.getTicksPerSecond(), true);
					time.doRemainingUpdateCount(() -> {
						for (int i = 0; i < time.getPhysicalScale(); i++) {
							for (PhysicalPOV physical : physicals) {
								physical.physics(world.enviroment);
								physical.applyVelocityIncreasing(physical.velocity, physical.velocityIncreasing, time.getTicksPerSecond());
								physical.applyVelocityIncreasing(physical.volution, physical.volutionIncreasing, time.getTicksPerSecond());
								physical.velocityMotionWithInertia(physical.position.coords, physical.velocity, physical.getInertia(), world.collider);
								physical.velocityMotionWithInertia(physical.rotation.coords, physical.volution, physical.getKinematics(), world.collider);
							}
							commands.command(player.getName(), player.selector(world, time.getTicksPerSecond(), false));
						}
					}, () -> {
						player.doForEachSeenBlock((x, y, z) -> world.calcNeedHide(x, y, z)/* fps x2 */);
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
			ExceptionsHandler.tryCatchVoid(() -> worldManager.saveWorld(world), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> Msgs.last.debug("Closed with exit code 0"), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> System.exit(0), (e) -> e.printStackTrace());
		}
	}
}
