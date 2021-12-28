package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glColor4d;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glScaled;
import static org.lwjgl.opengl.GL11.glTexCoord2f;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

import org.lwjgl.util.vector.Vector4f;

import ru.alexanderdv.modcraft.configs.SConfig;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.VectorD;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public class Block implements VerticalNormalised, Serializable {
	private static final long serialVersionUID = -4189237333497371824L;

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
	public static SConfig props;
	public static double sizeResolution = 100000000;

	private int id;

	public int getId() { return id; }

	private void writeObject(ObjectOutputStream oos) throws IOException { oos.writeInt(id); }

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException { id = ois.readInt(); }

	public Block(int x, int y, int z, int w, int id) { this.id = id; }

	public String getName() { return names.get(id) + ""; }

	public int[] getTextures() { return new int[] { id }; }

	public Vector4f[] getColors() { return new Vector4f[] { getProps().contains(",biomed,") ? new Vector4f(0, 1, 0, 1) : new Vector4f(1, 1, 1, 1) }; }

	public boolean isCollidable() { return !isGas() && !isLiquid() && !isVoid() && !isMeshed(); }

	public boolean isTransparent() { return (isGas() || isLiquid() || isVoid() || isMeshed()) && !getProps().contains(",!transparent,") || getProps().contains(",transparent,"); }

	public boolean isMeshed() { return getProps().contains(",meshed,"); }

	public boolean isSolid() { return !isVoid() && !isLiquid() && !isGas(); }

	public boolean isLiquid() { return getProps().contains(",liquid,"); }

	public boolean isGas() { return getProps().contains(",gas,"); }

	public boolean isVoid() { return id < 0; }

	public VectorD colorModifier;

	public void render(boolean[] hiddenSides) {
		if (colorModifier == null)
			colorModifier = new VectorD(4);
		glPushMatrix();
		glScaled(sizeResolution / (sizeResolution + 1), sizeResolution / (sizeResolution + 1), sizeResolution / (sizeResolution + 1));
		for (int i = 0; i < Side.values().length; i++)
			if (!hiddenSides[i] && id > 0 && !(getProps().contains(",onlysides,") && (i != 1 && i != 4)) && !(getProps().contains(",onlyquad,") && (i == 2 || i == 3))) {
				Side side = Side.values()[i];
				glBindTexture(3553, getTextures()[i % getTextures().length]);
				glColor4d(getColors()[i % getColors().length].x + colorModifier.getX(),

						getColors()[i % getColors().length].y - colorModifier.getX(),

						getColors()[i % getColors().length].z - colorModifier.getX(),

						getColors()[i % getColors().length].w - colorModifier.getW());
				if (getProps().contains(",cullface,"))
					glDisable(GL_CULL_FACE);
				else glEnable(GL_CULL_FACE);
				glPushMatrix();
				if (getProps().contains(",onlyquad,")) {
					glTranslated(0.05, 0, 0.05);
					glScaled(0.9, 1, 0.9);
				}
				if (getProps().contains(",offset,"))
					glTranslated(i == 1 ? 0.5 : 0, 0, i == 4 ? -0.5 : 0);
				if (getName().contains("cake"))
					glTranslated(i == 0 ? -0.05 : (i == 1 ? 0.05 : 0), i == 2 ? -0.05 : (i == 3 ? 0.05 : 0), i == 4 ? -0.05 : (i == 5 ? 0.05 : 0));
				glBegin(GL_QUADS);
				side.render();
				glEnd();
				glPopMatrix();
			}
		glPopMatrix();
	}

	public boolean isBreakable() { return isSolid() && !getName().contains("bedrock"); }

	public String getProps() { return "," + (!props.get(getName()).equals("undefined") ? props.get(getName()) : props.get(id + "")) + ","; }

	public double getDamage() { return (getProps().split("damage").length < 2 ? 0 : MathUtils.parseD(getProps().split("damage")[1].split(",")[0])); }

	public double getHeal() { return (getProps().split("heal").length < 2 ? 0 : MathUtils.parseD(getProps().split("heal")[1].split(",")[0])); }
}