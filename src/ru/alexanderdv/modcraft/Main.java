package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.glTranslated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JFrame;

import ru.alexanderdv.modcraft.World.GenerationType;
import ru.alexanderdv.utils.ExceptionsHandler;
import ru.alexanderdv.utils.MessageSystem.Msgs;
import ru.alexanderdv.utils.lwjgl.Timed;

public class Main {
	public static void main(String args[]) { new ModCraft(String.join(" ", args)).start(); }

	public static class ModCraft {
		public static class Player extends Controller implements Camera { public Player(String name) { super(name); } }

		TabWindowsBase windowBase;
		Textures textures;
		World world;
		Player player;
		ArrayList<Shader> shaders = new ArrayList<>();

		public ModCraft(String args) {
			Msgs.last = Msgs.msgs = new Msgs(args = this.getClass().getSimpleName() + " " + args) {};
			Custom defaults = Custom.defaults = new Custom(args.split(" ")), texturesCustom = new Custom(args.split(" "));

			windowBase = new TabWindowsBase(this.getClass().getSimpleName());
			windowBase.frame.init(Custom.hasArg("-displayInFrame", args) ? new JFrame() : null);
			windowBase.display.init(windowBase.frame.window);
			windowBase.output.init(new JFrame("Output"));
			windowBase.input.init();

			texturesCustom.loadBlocksWithTexturesTo(textures = new Textures(), Block.names);

			world = new World(16, 6, 16, GenerationType.RANDOM, GenerationType.AIR_ON_TOP);

			for (String name : args.split(" "))
				if (!name.startsWith("-") && name.length() > 0)
					(player = new Player(name)).custom = new Custom(defaults.playersS + "/" + name + "/" + defaults.controlsS + defaults.cfgExtS);

			shaders.add(new FlickingShader());
		}

		public interface Shader extends Timed {}

		public static class FlickingShader implements Shader {
			double overlayTexturesFlickingModifier = 1, currentFlickingOffset = 0;
			Random random = new Random();

			@Override
			public void render() { glTranslated(currentFlickingOffset * 0, currentFlickingOffset * 0, currentFlickingOffset * 1); }

			@Override
			public void update() { currentFlickingOffset = (random.nextDouble() - 0.5d) / 1001d * overlayTexturesFlickingModifier; }
		}

		public static class Time {
			private double nano = 1, resolutionModifier = 1d, ups = 50, lastRender = getNow(), lastUpdate = getNow(), needUpdateCount = 0, scale = 1, start = getNow();

			public double getResolution() { return (nano != 0 ? 1000000000d : 1000d) * resolutionModifier; }

			public double getNow() { return (nano != 0 ? System.nanoTime() / 1000000d : System.currentTimeMillis()) / 1000d * getResolution(); }

			public double getWork() { return getNow() - start; }

			public double fps() { return getResolution() / -(lastRender - (lastRender = getNow())); }

			public void doNeedUpdateCount(Runnable update, Runnable inLast) {
				for (needUpdateCount += -(lastUpdate - (lastUpdate = getNow())) / getResolution() * ups; needUpdateCount > 0; needUpdateCount--) {
					update.run();
					if (needUpdateCount < 1)
						inLast.run();
				}
			}

			public double getScale() { return scale; }

			public void setScale(double scale) { this.scale = scale; }
		}

		Time time;

		public void start() {
			time = new Time();
			ExceptionsHandler handler = new ExceptionsHandler();
			while (!stopped())
				handler.tryPrint(() -> {
					time.doNeedUpdateCount(() -> player.controls(windowBase.display, windowBase.input), () -> {
						player.doForEachSeenBlock((x, y, z) -> world.calcNeedHide(x, y, z)/* fps x2 */);
						shaders.forEach((shader) -> shader.update());
					});

					player.openEyes(windowBase.display.getWidth(), windowBase.display.getHeight(), 100d);
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
					windowBase.print("FPS: " + time.fps() + "\nRotation" + Arrays.toString(player.rotation.coords));
					windowBase.repaint();
					return null;
				});
			endProgram();
		}

		private boolean stopped() { return player.ended || windowBase.isDestroyed(); }

		private void endProgram() {
			windowBase.destroy();
			System.exit(0);
		}
	}
}
