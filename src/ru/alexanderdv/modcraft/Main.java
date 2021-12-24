package ru.alexanderdv.modcraft;

import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;

import ru.alexanderdv.modcraft.Config.SConfig;
import ru.alexanderdv.modcraft.Controller.UserController;
import ru.alexanderdv.modcraft.World.Generation;
import ru.alexanderdv.modcraft.World.GenerationType;
import ru.alexanderdv.utils.ExceptionsHandler;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.MessageSystem.Msgs;
import ru.alexanderdv.utils.lwjgl.Timed;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public class Main {
	public static void main(String args[]) { new ModCraft(String.join(" ", args)).start(); }

	public static class ModCraft implements VerticalNormalised {
		public static class Player extends UserController implements Camera { public Player(String name) { super(name); } }

		FramesManager frames;
		Textures textures;
		World world;
		Player player;
		ArrayList<PhysicalPOV> physicals = new ArrayList<>();
		ArrayList<Shader> shaders = new ArrayList<>();

		public ModCraft(String args) {
			Msgs.last = Msgs.msgs = new Msgs(args = this.getClass().getSimpleName() + " " + args) {};
			Config<String> defaults = Config.defaults = new Config<String>(args.split(" "));

			frames = new FramesManager(this.getClass().getSimpleName());
			frames.displayFrame.init(Config.hasArg("-displayInFrame", args) ? new JFrame() : null);
			frames.display.init(frames.displayFrame.frame);// TODO If rendering in other thread, it's also
			frames.debugFrame.init(new JFrame("F3"));
			frames.input.init(frames.display);

			new SConfig(defaults.configsS + "/" + defaults.texturesS + defaults.cfgExtS).apply(textures = new Textures(), Block.names);

			try {
				world = loadWorld("world.txt");
			} catch (Exception e) {
				Msgs.last.debug(e);
				world = generateWorld(args);
			}

			for (String name : args.split(" "))
				if (!name.startsWith("-") && name.length() > 0) {
					(player = new Player(name)).controls = new SConfig(defaults.playersS + "/" + name + "/" + defaults.controlsS + defaults.cfgExtS);
					player.input = frames.input;
					player.position.coords = new double[] { world.size[0] / 2, world.size[1] - 1, world.size[2] / 2 };
					player.rotation.coords[0] = 90;
					physicals.add(player);
				}

			shaders.add(new FlickingShader());
		}

		String[] sp = { "\n", ";", "," };

		public World loadWorld(String path) {
			String saveTxt = new SConfig(path).readConfigText(Config.defaults.savesPath + path);
			String[][][] saveIds = new String[saveTxt.split(sp[0]).length][saveTxt.split(sp[0])[0].split(sp[1]).length][saveTxt.split(sp[0])[0].split(sp[1])[0].split(sp[2]).length];
			for (int x = 0; x < saveTxt.split(sp[0]).length; x++)
				if (saveTxt.split(sp[0])[x] != null && !saveTxt.split(sp[0])[x].equals(""))
					for (int y = 0; y < saveTxt.split(sp[0])[x].split(sp[1]).length; y++)
						if (saveTxt.split(sp[0])[x].split(sp[1])[y] != null && !saveTxt.split(sp[0])[x].split(sp[1])[y].equals(""))
							try {
								saveIds[x][y] = saveTxt.split(sp[0])[x].split(sp[1])[y].split(sp[2]);
							} catch (Exception e) {
								e.printStackTrace();
							}
			if (saveIds.length < 2 || saveIds[0].length < 2 || saveIds[0][0].length < 2)
				throw new RuntimeException("Zero world size");
			World loadedWorld = new World(saveIds.length, saveIds[0].length, saveIds[0][0].length);
			for (int x = 0; x < saveIds.length; x++)
				for (int y = 0; y < saveIds[x].length; y++)
					for (int z = 0; z < saveIds[x][y].length; z++)
						try {
							loadedWorld.setBlock(x, y, z, MathUtils.parseI(saveIds[x][y][z]));
						} catch (Exception e) {}
			return loadedWorld;
		}

		public void saveWorld(String path) {
			Config<String> cfg = null;
			try {
				cfg = new Config<String>("".split(""));
			} catch (Exception e) {}
			String text = "";
			for (int x = 0; x < world.size[0]; x++, text += sp[0])
				for (int y = 0; y < world.size[1]; y++, text += sp[1])
					for (int z = 0; z < world.size[2]; z++, text += sp[2])
						text += world.getBlock(x, y, z).id;
			cfg.saveConfigText(path, text);
		}

		public World generateWorld(String args) {
			Config<String> worldCustom = new Config<String>(args.split(" "));
			String[] worldCfgLines = worldCustom.readConfigLines(worldCustom.configsS + "/world" + worldCustom.cfgExtS);
			Generation[] generations = new Generation[worldCfgLines.length - 5];
			for (int i = 0; i < generations.length; i++)
				try {
					generations[i] = GenerationType.valueOf(worldCfgLines[i + 5].toUpperCase());
				} catch (Exception e) {
					final int o = i;
					generations[i] = new Generation() {
						@Override
						public Block getBlock(int x, int y, int z, World w) {
							if (y < MathUtils.parseI(worldCfgLines[o + 5].split(":")[0]))
								return new Block(x, y, z, w, MathUtils.parseI(worldCfgLines[o + 5].split(":")[1]));
							else return w.getBlock(x, y, z);
						}

					};
				}
			World generatedWorld = new World(MathUtils.parseI(worldCfgLines[0]), MathUtils.parseI(worldCfgLines[1]), MathUtils.parseI(worldCfgLines[2]), generations);
			generatedWorld.border = worldCfgLines[3].toLowerCase().contains("true");
			generatedWorld.blocksCollision = worldCfgLines[4].toLowerCase().contains("true");
			return generatedWorld;
		}

		public interface Shader extends Timed {}

		public static class FlickingShader implements Shader, VerticalNormalised {
			double overlayTexturesFlickingModifier = 1, currentFlickingOffset = 0;
			Random random = new Random();

			@Override
			public void render() { glTranslated(currentFlickingOffset * 0, currentFlickingOffset * 0, currentFlickingOffset * 1); }

			@Override
			public void update() { currentFlickingOffset = (random.nextDouble() - 0.5d) / 1001d * overlayTexturesFlickingModifier; }
		}// TODO 0.5 kinematics divide world picture to 2, mind this bug how non strict
			// blocks

		public static class Time {
			private double ticksPerSecond = 50, physicalScale = 1;

			public double getTicksPerSecond() { return ticksPerSecond; }

			public double getPhysicalScale() { return physicalScale; }

			public void changePhysicalScale(double scale) { this.physicalScale = scale; }

			public double countRendersPerSecond() { return getResolution() / -(lastRender - (lastRender = countNowTime())); }

			public double countFramesPerSecond() { return countRendersPerSecond(); }

			public double fps() { return countFramesPerSecond(); }

			private double nano = 1, resolutionModifier = 1d;

			public double getResolution() { return (nano != 0 ? 1000000000d : 1000d) * resolutionModifier; }

			public double countNowTime() { return (nano != 0 ? System.nanoTime() / 1000000d : System.currentTimeMillis()) / 1000d * getResolution(); }

			public double countWorkTime() { return countNowTime() - start; }

			private double start = countNowTime(), lastRender = countNowTime(), lastUpdate = countNowTime(), remainingUpdateCount = 0;

			public void doRemainingUpdateCount(Runnable updateMethod, Runnable doInLastUpdate) {
				for (remainingUpdateCount += -(lastUpdate - (lastUpdate = countNowTime())) / getResolution() * ticksPerSecond; remainingUpdateCount > 0; remainingUpdateCount--) {
					updateMethod.run();
					if (remainingUpdateCount < 1)
						doInLastUpdate.run();
				}
			}
		}

		Time time;

		public void start() {
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
							player.selector(world, time.getTicksPerSecond(), false);
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

		private boolean stopped() { return player.ended || frames.isDestroyed(); }

		private void endProgram() {
			ExceptionsHandler.tryCatchVoid(() -> frames.destroy(), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> saveWorld("world.txt"), (e) -> e.printStackTrace());
			ExceptionsHandler.tryCatchVoid(() -> System.exit(0), (e) -> e.printStackTrace());
		}
	}
}
