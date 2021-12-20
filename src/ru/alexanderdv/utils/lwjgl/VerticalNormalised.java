package ru.alexanderdv.utils.lwjgl;

public interface VerticalNormalised {
	default void glTranslatef(float x, float y, float z) { org.lwjgl.opengl.GL11.glTranslatef(-x, +y, -z); }

	default void glTranslated(double x, double y, double z) { org.lwjgl.opengl.GL11.glTranslated(-x, +y, -z); }
}