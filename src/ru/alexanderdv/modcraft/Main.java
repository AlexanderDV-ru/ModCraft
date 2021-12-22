package ru.alexanderdv.modcraft;

import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;

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
			Custom defaults = Custom.defaults = new Custom(args.split(" ")), texturesCustom = new Custom(args.split(" "));

			frames = new FramesManager(this.getClass().getSimpleName());
			frames.displayFrame.init(Custom.hasArg("-displayInFrame", args) ? new JFrame() : null);
			frames.display.init(frames.displayFrame.frame);// TODO If rendering in other thread, it's also
			frames.debugFrame.init(new JFrame("F3"));
			frames.input.init(frames.display);

			texturesCustom.loadBlocksWithTexturesTo(textures = new Textures(), Block.names);

			Custom worldCustom = new Custom(args.split(" "));
			String[] worldCfgLines = worldCustom.readCfgCustom(worldCustom.configsS + "/world" + worldCustom.cfgExtS);
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
			world = new World(MathUtils.parseI(worldCfgLines[0]), MathUtils.parseI(worldCfgLines[1]), MathUtils.parseI(worldCfgLines[2]), generations);
			world.border = worldCfgLines[3].toLowerCase().contains("true");
			world.blocksCollision = worldCfgLines[4].toLowerCase().contains("true");

			for (String name : args.split(" "))
				if (!name.startsWith("-") && name.length() > 0) {
					(player = new Player(name)).custom = new Custom(defaults.playersS + "/" + name + "/" + defaults.controlsS + defaults.cfgExtS);
					player.input = frames.input;
					player.position.coords = new double[] { world.size[0] / 2, world.size[1] - 1, world.size[2] / 2 };
					player.rotation.coords[0] = 90;
					physicals.add(player);
				}

			shaders.add(new FlickingShader());
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
			private double ticksPerSecond = 50, physicalScale = 50;

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

					time.doRemainingUpdateCount(() -> {
						for (PhysicalPOV physical : physicals)
							for (int i = 0; i < time.getPhysicalScale(); i++) {
								physical.physics(world.enviroment);
								physical.applyVelocityIncreasing(physical.velocity, physical.velocityIncreasing, time.getTicksPerSecond());
								physical.applyVelocityIncreasing(physical.volution, physical.volutionIncreasing, time.getTicksPerSecond());
								physical.velocityMotionWithInertia(physical.position.coords, physical.velocity, physical.getInertia(), world.collider);
								physical.velocityMotionWithInertia(physical.rotation.coords, physical.volution, physical.getKinematics(), world.collider);
							}
					}, () -> {
						player.doForEachSeenBlock((x, y, z) -> world.calcNeedHide(x, y, z)/* fps x2 */);
						shaders.forEach((shader) -> shader.update());
					});

					player.openEyes(frames.display.getWidth(), frames.display.getHeight(), 100d);
					shaders.forEach((shader) -> shader.render());
					player.pointOfVision(player);
					player.selectRenderDirectionByRotation(world);
					player.doForEachSeenBlock((x, y, z) -> {
						if (world.getBlock(x, y, z).id != 0) {
							glTranslated(x, y, z);
							world.getBlock(x, y, z).render();
							glTranslated(-x, -y, -z);
						}
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
			frames.destroy();
			System.exit(0);
		}
	}
}
