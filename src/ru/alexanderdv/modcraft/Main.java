package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glTranslated;
import static org.lwjgl.opengl.GL11.glVertex3d;

import java.util.Random;

import javax.swing.JFrame;

import org.lwjgl.opengl.GL11;

public class Main {
	public static void main(String args[]) { new ModCraft(false).start(); }

	public static class ModCraft {
		TabWindowsBase windowBase;

		public static class Player extends Controller implements Camera {}

		Player player;
		World world;

		public ModCraft(boolean displayInFrame) {
			windowBase = new TabWindowsBase(this.getClass().getSimpleName());
			windowBase.frame.init(displayInFrame ? new JFrame() : null);
			windowBase.display.init(windowBase.frame.window);
			windowBase.output.init(new JFrame("Output"));
			windowBase.input.init();

			player = new Player();
			world = new World(64, 256, 64);
			Random r = new Random();
			for (int x = 1; x < world.xSize - 1; x++)
				for (int z = 1; z < world.zSize - 1; z++)
					for (int y = 1; y < 10 - 1; y++)
						world.setBlock(x, y, z, (r.nextInt() % 20 + 20) % 20);
		}

		long lastTime = System.currentTimeMillis();

		public void start() {
			while (!stopped())
				try {
					player.openEyes(windowBase.display.getWidth(), windowBase.display.getHeight(), 100d);
					player.pointOfVision(player);
					player.controls(windowBase.display, windowBase.input);
					renderBlocks();
					player.closeEyes();
					windowBase.print("FPS: " + 1000f / -(lastTime - (lastTime = System.currentTimeMillis())));
					windowBase.repaint();
				} catch (Exception e) {
					e.printStackTrace();
				}
			endProgram();
		}

		private boolean stopped() { return player.ended || windowBase.isDestroyed(); }

		public void endProgram() {
			windowBase.destroy();
			System.exit(0);
		}

		private void renderBlocks() {
			for (String name : Textures.names)
				if (name.contains(".png"))
					textures.load("/assets/" + windowBase.name + "/textures/" + name);

			player.selectRenderDirectionByRotation(world);
			player.doForEachBlockInFieldOfVision((x, y, z) -> {
				if (world.getBlock(x, y, z).id != 0) {
					GL11.glPushMatrix();
					glTranslated(x, y, z);
					GL11.glBindTexture(3553, world.getBlock(x, y, z).id);
					if (world.getBlock(x, y, z).id == 16)
						GL11.glColor4f(0, 1, 0, 1);
					else GL11.glColor4f(1, 1, 1, 1);
					renderBlock(world.needHide(x, y, z));
					glPopMatrix();
				}
			});
		}

		public void renderBlock(boolean[] isNeedHide) {
			if (!isNeedHide[0]) {
				// side1
				glBegin(GL_QUADS);
				GL11.glTexCoord2d(0, 0);
				glVertex3d(+0.5, +0.5, +0.5);
				GL11.glTexCoord2d(0, 1);
				glVertex3d(-0.5, +0.5, +0.5);
				GL11.glTexCoord2d(1, 1);
				glVertex3d(-0.5, +0.5, -0.5);
				GL11.glTexCoord2d(1, 0);
				glVertex3d(+0.5, +0.5, -0.5);
				glEnd();
			}
			if (!isNeedHide[1]) {
				// side2
				glBegin(GL_QUADS);
				GL11.glTexCoord2d(0, 0);
				glVertex3d(-0.5, -0.5, -0.5);
				GL11.glTexCoord2d(0, 1);
				glVertex3d(+0.5, -0.5, -0.5);
				GL11.glTexCoord2d(1, 1);
				glVertex3d(+0.5, -0.5, +0.5);
				GL11.glTexCoord2d(1, 0);
				glVertex3d(-0.5, -0.5, +0.5);
				glEnd();
			}
			if (!isNeedHide[2]) {
				// side3
				glBegin(GL_QUADS);
				GL11.glTexCoord2d(0, 0);
				glVertex3d(+0.5, +0.5, +0.5);
				GL11.glTexCoord2d(0, 1);
				glVertex3d(-0.5, +0.5, +0.5);
				GL11.glTexCoord2d(1, 1);
				glVertex3d(-0.5, -0.5, +0.5);
				GL11.glTexCoord2d(1, 0);
				glVertex3d(+0.5, -0.5, +0.5);
				glEnd();
			}
			if (!isNeedHide[3]) {
				// side4
				glBegin(GL_QUADS);
				GL11.glTexCoord2d(0, 0);
				glVertex3d(-0.5, -0.5, -0.5);
				GL11.glTexCoord2d(0, 1);
				glVertex3d(+0.5, -0.5, -0.5);
				GL11.glTexCoord2d(1, 1);
				glVertex3d(+0.5, +0.5, -0.5);
				GL11.glTexCoord2d(1, 0);
				glVertex3d(-0.5, +0.5, -0.5);
				glEnd();
			}
			if (!isNeedHide[4]) {
				// side5
				glBegin(GL_QUADS);
				GL11.glTexCoord2d(0, 0);
				glVertex3d(+0.5, +0.5, +0.5);
				GL11.glTexCoord2d(0, 1);
				glVertex3d(+0.5, -0.5, +0.5);
				GL11.glTexCoord2d(1, 1);
				glVertex3d(+0.5, -0.5, -0.5);
				GL11.glTexCoord2d(1, 0);
				glVertex3d(+0.5, +0.5, -0.5);
				glEnd();
			}
			if (!isNeedHide[5]) {
				// side6
				glBegin(GL_QUADS);
				GL11.glTexCoord2d(0, 0);
				glVertex3d(-0.5, -0.5, -0.5);
				GL11.glTexCoord2d(0, 1);
				glVertex3d(-0.5, +0.5, -0.5);
				GL11.glTexCoord2d(1, 1);
				glVertex3d(-0.5, +0.5, +0.5);
				GL11.glTexCoord2d(1, 0);
				glVertex3d(-0.5, -0.5, +0.5);
				glEnd();
			}
		}

		static {
			Textures.names[0] = "  air";
			Textures.names[1] = "  stone.png";
			Textures.names[2] = "  grass.png";
			Textures.names[3] = "  dirt.png";
			Textures.names[4] = "  cobblestone.png";
			Textures.names[5] = "  sand.png";
			Textures.names[6] = "  gravel.png";
			Textures.names[7] = "  bedrock.png";
			Textures.names[8] = "  water.png";
			Textures.names[9] = "  sponge.png";
			Textures.names[10] = " lava.png";
			Textures.names[11] = " tnt_top.png";
			Textures.names[12] = " iron_ore.png";
			Textures.names[13] = " lapis_ore.png";
			Textures.names[14] = " emerald_ore.png";
			Textures.names[15] = " sapling.png";
			Textures.names[16] = " leaves.png";
			Textures.names[17] = " planks.png";
			Textures.names[18] = " wood_side.png";
			Textures.names[19] = " wood_top.png";
			Textures.names[20] = " glass.png";
			Textures.names[21] = " selector.png";
			Textures.names[22] = " white";
			for (int i = 0; i < Block.names.length; i++)
				Block.names[i] = (Textures.names[i] = Textures.names[i].replace(" ", "")).replace(".png", "");
		}
		Textures textures = new Textures();
	}
}
