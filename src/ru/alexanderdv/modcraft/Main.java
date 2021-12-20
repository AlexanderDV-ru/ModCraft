package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.glTranslated;

import javax.swing.JFrame;

import ru.alexanderdv.modcraft.World.GenerationType;

public class Main {
	public static void main(String args[]) { new ModCraft(false).start(); }

	public static class ModCraft {
		public static class Player extends Controller implements Camera {}

		TabWindowsBase windowBase;
		Textures textures;
		World world;
		Player player;

		public ModCraft(boolean displayInFrame) {
			windowBase = new TabWindowsBase(this.getClass().getSimpleName());
			windowBase.frame.init(displayInFrame ? new JFrame() : null);
			windowBase.display.init(windowBase.frame.window);
			windowBase.output.init(new JFrame("Output"));
			windowBase.input.init();

			Block.names.put(0, "  air");
			Block.names.put(1, "  stone.png");
			Block.names.put(2, "  grass.png");
			Block.names.put(3, "  dirt.png");
			Block.names.put(4, "  cobblestone.png");
			Block.names.put(5, "  sand.png");
			Block.names.put(6, "  gravel.png");
			Block.names.put(7, "  bedrock.png");
			Block.names.put(8, "  water.png");
			Block.names.put(9, "  sponge.png");
			Block.names.put(10, " lava.png");
			Block.names.put(11, " tnt_top.png");
			Block.names.put(12, " iron_ore.png");
			Block.names.put(13, " lapis_ore.png");
			Block.names.put(14, " emerald_ore.png");
			Block.names.put(15, " sapling.png");
			Block.names.put(16, " leaves.png");
			Block.names.put(17, " planks.png");
			Block.names.put(18, " wood_side.png");
			Block.names.put(19, " wood_top.png");
			Block.names.put(20, " glass.png");
			Block.names.put(21, " selector.png");
			Block.names.put(22, " wool.png");
			Block.names.put(23, " white");
			textures = new Textures();
			for (int i = 0; i < Block.names.size(); i++) {
				if (Block.names.get(i).replace(" ", "").contains(".png"))
					textures.load("assets/" + windowBase.name + "/textures/" + Block.names.get(i).replace(" ", ""));
				Block.names.put(i, Block.names.get(i).replace(" ", "").replace(".png", ""));
			}

			world = new World(64, 16, 64, GenerationType.RANDOM);

			player = new Player();
		}

		public static class Time {
			private double nano = 1, resolutionModifier = 1d, updateCount = 50, lastRender = getNow(), lastUpdate = getNow(), needUpdateCount = 0, scale = 1;

			public double getResolution() { return (nano != 0 ? 1000000000d : 1000d) * resolutionModifier; }

			public double getNow() { return (nano != 0 ? System.nanoTime() / 1000000d : System.currentTimeMillis()) / 1000d * getResolution(); }

			public double fps() { return getResolution() / -(lastRender - (lastRender = getNow())); }

			public void doNeedUpdateCount(Runnable update, Runnable inLast) {
				for (needUpdateCount += -(lastUpdate - (lastUpdate = getNow())) / getResolution() * updateCount; needUpdateCount > 0; needUpdateCount--) {
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
			while (!stopped())
				try {
					player.openEyes(windowBase.display.getWidth(), windowBase.display.getHeight(), 100d);
					player.pointOfVision(player);

					time.doNeedUpdateCount(() -> player.controls(windowBase.display, windowBase.input), () -> player.doForEachSeenBlock((x, y, z) -> world.calcNeedHide(x, y, z)/* fps x2 */));

					player.selectRenderDirectionByRotation(world);
					player.doForEachSeenBlock((x, y, z) -> {
						if (world.getBlock(x, y, z).id != 0) {
							glTranslated(x, y, z);
							world.getBlock(x, y, z).render();
							glTranslated(-x, -y, -z);
						}
					});

					player.closeEyes();
					windowBase.print("FPS: " + time.fps());
					windowBase.repaint();
				} catch (Exception e) {
					e.printStackTrace();
				}
			endProgram();
		}

		private boolean stopped() { return player.ended || windowBase.isDestroyed(); }

		private void endProgram() {
			windowBase.destroy();
			System.exit(0);
		}
	}
}
