package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glScaled;
import static org.lwjgl.opengl.GL11.glTexCoord2f;

import java.util.HashMap;

import org.lwjgl.util.vector.Vector4f;

import ru.alexanderdv.utils.VectorD;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public class Block implements VerticalNormalised {
	private static int _enum_counter = 0;

	public static enum Side implements VerticalNormalised {// { 1, 0 }, { 0, 0 }, { 0, 1 }, { 1, 1 } rotated down
		RIGHT(/*   */0, 1, new double[][] { { 0, 1 }, { 0, 0 }, { 1, 0 }, { 1, 1 } }, new float[][] { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 0, 0 } }),
		LEFT(/*    */0, 0, new double[][] { { 0, 0 }, { 0, 1 }, { 1, 1 }, { 1, 0 } }, new float[][] { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 0, 0 } }),
		TOP(/*     */1, 1, new double[][] { { 1, 1 }, { 1, 0 }, { 0, 0 }, { 0, 1 } }, new float[][] { { 1, 0 }, { 0, 0 }, { 0, 1 }, { 1, 1 } }),
		BOTTOM(/*  */1, 0, new double[][] { { 1, 1 }, { 0, 1 }, { 0, 0 }, { 1, 0 } }, new float[][] { { 0, 0 }, { 0, 1 }, { 1, 1 }, { 1, 0 } }),
		FORWARD(/* */2, 1, new double[][] { { 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, 1 } }, new float[][] { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 0, 0 } }),
		BACK(/*    */2, 0, new double[][] { { 1, 0 }, { 0, 0 }, { 0, 1 }, { 1, 1 } }, new float[][] { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 0, 0 } }/* rotated up */);

		public int id;
		protected VectorD direction;
		protected double[][] vertices;
		protected float[][] texturing;

		public void render() {
			for (int p = 0; p < vertices.length; p++) {
				glTexCoord2f(texturing[p][0], texturing[p][1]);
				glVertex3d(vertices[p][0] + direction.getX(), vertices[p][1] + direction.getY(), vertices[p][2] + direction.getZ());
			}
		}

		Side(int axis, double value, double[][] verticesMatrix, float[][] texturingMatrix) {
			this.direction = new VectorD(3);
			this.direction.coords[axis] = value;
			this.vertices = new double[4][3];
			for (int n = 0; n < vertices.length; n++)
				vertices[n] = new double[] { axis == 0 ? 0 : verticesMatrix[n][0], axis == 1 ? 0 : (verticesMatrix[n][axis == 2 ? 1 : 0]), axis == 2 ? 0 : verticesMatrix[n][1] };
			this.texturing = texturingMatrix;

			this.id = _enum_counter++;
		}

		public static Side parse(String string) {
			try {
				return values()[Integer.parseInt(string)];
			} catch (Exception e) {
				return valueOf(string);
			}
		}
	}

	public static final HashMap<Integer, String> names = new HashMap<>();
	public static double sizeResolution = 100000000;

	World world;
	int x, y, z, w;
	int id;

	public Block(World world, int x, int y, int z, int w, int id) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		this.id = id;
	}

	public String getName() { return names.get(id); }

	public int[] getTextures() { return new int[] { id }; }

	public Vector4f[] getColors() { return new Vector4f[] { getName().contains("leaves") || getName().contains("grass") ? new Vector4f(0, 1, 0, 1) : new Vector4f(1, 1, 1, 1) }; }

	public boolean isCollidable() { return !isGas() && !isLiquid() && !isVoid() && !isMeshed(); }

	public boolean isTransparent() { return isGas() || isLiquid() || isVoid() || isMeshed() || getName().contains("glass"); }

	public boolean isMeshed() { return getName().contains("leaves") || getName().contains("sapling"); }

	public boolean isSolid() { return !isVoid() && !isLiquid() && !isGas(); }

	public boolean isLiquid() { return getName().contains("water") || getName().contains("lava"); }

	public boolean isGas() { return getName().contains("air"); }

	public boolean isVoid() { return id < 0; }

	float opacity = 1;

	public void render(boolean[] hiddenSides) {
		glScaled(sizeResolution / (sizeResolution + 1), sizeResolution / (sizeResolution + 1), sizeResolution / (sizeResolution + 1));
		for (int i = 0; i < Side.values().length; i++)
			if (!hiddenSides[i] && !(getName().contains("sapling") && (i != 1 && i != 4))) {
				Side side = Side.values()[i];
				glBindTexture(3553, getTextures()[i % getTextures().length]);
				glColor4f(getColors()[i % getColors().length].x, getColors()[i % getColors().length].y, getColors()[i % getColors().length].z, opacity);
				if (getName().contains("sapling")) {
					glTranslated(i == 1 ? 0.5 : 0, 0, i == 4 ? -0.5 : 0);
					glDisable(GL_CULL_FACE);
				} else glEnable(GL_CULL_FACE);
				glBegin(GL_QUADS);
				side.render();
				glEnd();
				if (getName().contains("sapling"))
					glTranslated(i == 1 ? -0.5 : 0, 0, i == 4 ? 0.5 : 0);
			}
		glScaled((sizeResolution + 1) / sizeResolution, (sizeResolution + 1) / sizeResolution, (sizeResolution + 1) / sizeResolution);
	}

	public boolean isBreakable() { return isSolid() && !getName().contains("bedrock"); }
}