package ru.alexanderdv.utils.lwjgl;

/**
 * It is crutch using defaults interface, how import, not class or interface.
 * <br>
 * Need to y-axis in LWJGL go from down to up in world, not from display top.
 */
public interface VerticalNormalised {
	default void glTranslatef(float x, float y, float z) { org.lwjgl.opengl.GL11.glTranslatef(-x, +y, -z); }

	default void glTranslated(double x, double y, double z) { org.lwjgl.opengl.GL11.glTranslated(-x, +y, -z); }

	default void glVertex3d(double x, double y, double z) { org.lwjgl.opengl.GL11.glVertex3d(-x, +y, -z); }

	default void glVertex3f(float x, float y, float z) { org.lwjgl.opengl.GL11.glVertex3f(-x, +y, -z); }
}