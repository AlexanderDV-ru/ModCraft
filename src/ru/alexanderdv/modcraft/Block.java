package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glVertex3d;

import java.util.HashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Renderable;
import org.lwjgl.util.vector.Vector4f;

import ru.alexanderdv.utils.VectorD;

public class Block {
	private static int _enum_counter = 0;

	public static interface Side extends Renderable {}

	public static enum Side6 implements Side {
		TOP(00000 + 0, 1, 0, new double[][] { { 1, 0, 1 }, { 1, 0, 0 }, { 0, 0, 0 }, { 0, 0, 1 } }, new float[][] { { 1, 0 }, { 0, 0 }, { 0, 1 }, { 1, 1 } }),
		BOTTOM(00 + 0, 0, 0, new double[][] { { 1, 0, 1 }, { 0, 0, 1 }, { 0, 0, 0 }, { 1, 0, 0 } }, new float[][] { { 0, 0 }, { 0, 1 }, { 1, 1 }, { 1, 0 } }),
		RIGHT(000 + 1, 0, 0, new double[][] { { 0, 0, 1 }, { 0, 0, 0 }, { 0, 1, 0 }, { 0, 1, 1 } }, new float[][] { { 0, 1 }, { 1, 1 }, { 1, 0 }, { 0, 0 } }),
		LEFT(0000 + 0, 0, 0, new double[][] { { 0, 0, 0 }, { 0, 0, 1 }, { 0, 1, 1 }, { 0, 1, 0 } }, new float[][] { { 1, 1 }, { 1, 0 }, { 0, 0 }, { 0, 1 } }),
		FORWARD(0 + 0, 0, 1, new double[][] { { 0, 0, 0 }, { 1, 0, 0 }, { 1, 1, 0 }, { 0, 1, 0 } }, new float[][] { { 1, 0 }, { 0, 0 }, { 0, 1 }, { 1, 1 } }/* copy of TOP */),
		BACK(0000 + 0, 0, 0, new double[][] { { 1, 0, 0 }, { 0, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 } }, new float[][] { { 1, 0 }, { 0, 0 }, { 0, 1 }, { 1, 1 } }/* copy of TOP */);

		public int id;
		protected VectorD sideDirectionVector;
		protected double[][] vertices;
		protected float[][] texturing;

		@Override
		public void render() {
			for (int p = 0; p < vertices.length; p++) {
				glTexCoord2f(texturing[p][0], texturing[p][1]);
				glVertex3d(vertices[p][0] + sideDirectionVector.getX() - 0.5d, vertices[p][1] + sideDirectionVector.getY() - 0.5d, vertices[p][2] + sideDirectionVector.getZ() - 0.5d);
			}
		}

		Side6(double x, double y, double z, double[][] verticesMatrix, float[][] texturingMatrix) {
			this.sideDirectionVector = new VectorD(x, y, z);
			this.vertices = verticesMatrix;
			this.texturing = texturingMatrix;

			this.id = _enum_counter++;
		}

		public static Side6 parse(String string) {
			try {
				return values()[Integer.parseInt(string)];
			} catch (Exception e) {
				return valueOf(string);
			}
		}
	}

	public static final HashMap<Integer, String> names = new HashMap<>();
	public static double sizeResolution = 100000000;

	int id, x, y, z;
	World w;

	public Block(int x, int y, int z, World w, int id) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		this.id = id;
	}

	public String getName() { return names.get(id); }

	public int[] getTextures() { return new int[] { id }; }

	public Vector4f[] getColors() { return new Vector4f[] { getName().contains("leaves") || getName().contains("grass") ? new Vector4f(0, 1, 0, 1) : new Vector4f(1, 1, 1, 1) }; }

	public boolean isTransparent() { return isGas() || isLiquid() || isVoid() || isMeshed() || getName().contains("glass"); }

	public boolean isMeshed() { return getName().contains("leaves") || getName().contains("sapling"); }

	public boolean isSolid() { return !isVoid() && !isLiquid() && !isGas(); }

	public boolean isLiquid() { return getName().contains("water") || getName().contains("lava"); }

	public boolean isGas() { return getName().contains("air"); }

	public boolean isVoid() { return id < 0; }

	public void render() {
		GL11.glScaled(sizeResolution / (sizeResolution + 1), sizeResolution / (sizeResolution + 1), sizeResolution / (sizeResolution + 1));
		for (int i = 0; i < Side6.values().length; i++)
			if (!w.isNeedHide(x, y, z)[i]) {
				GL11.glBindTexture(3553, getTextures()[i % getTextures().length]);
				GL11.glColor4f(getColors()[i % getColors().length].x, getColors()[i % getColors().length].y, getColors()[i % getColors().length].z, getColors()[i % getColors().length].w);
				GL11.glBegin(GL_QUADS);
				Side6.values()[i].render();
				GL11.glEnd();
			}
		GL11.glScaled((sizeResolution + 1) / sizeResolution, (sizeResolution + 1) / sizeResolution, (sizeResolution + 1) / sizeResolution);
	}
}