package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotated;

import org.lwjgl.util.glu.GLU;

import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public interface Camera extends VerticalNormalised {

	public default void openEyes(double width, double height, double visionDistance) {
		glPushMatrix();
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		GLU.gluPerspective(70, (float) width / (float) height, 0.03f, (float) visionDistance);
		glMatrixMode(GL_MODELVIEW);
		glEnable(GL_DEPTH_TEST);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glLoadIdentity();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(3553);
	}

	public default void pointOfVision(POV p) {
		glRotated(p.rotation.coords[0] = MathUtils.loopD(p.rotation.getX(), 0, 360), 1, 0, 0);
		glRotated(p.rotation.coords[1] = MathUtils.loopD(p.rotation.getY(), 0, 360), 0, 1, 0);
		glTranslated(-p.position.getX(), -p.position.getY(), -p.position.getZ());
	}

	public default void closeEyes() { glPopMatrix(); }
}