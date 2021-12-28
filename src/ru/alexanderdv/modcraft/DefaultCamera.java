package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotated;
import static org.lwjgl.util.glu.GLU.gluPerspective;

import ru.alexanderdv.modcraft.interfaces.Camera;
import ru.alexanderdv.utils.MathUtils;

public class DefaultCamera implements Camera {

	@Override
	public void openEyes(float fov, float width, float height, float visionDistance) {
		glPushMatrix();
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(fov, width / height, 0.03f, visionDistance);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(3553);
		glPushMatrix();
	}

	@Override
	public void pointOfView(POV p) {
		glPopMatrix();
		glPushMatrix();
		glRotated(p.rotation.coords[0] = MathUtils.loopD(p.rotation.getX(), 0, 360), 1, 0, 0);
		glRotated(p.rotation.coords[1] = MathUtils.loopD(p.rotation.getY(), 0, 360), 0, 1, 0);
		glTranslated(-p.position.getX(), -p.position.getY(), -p.position.getZ());
	}

	@Override
	public void closeEyes() {
		glPopMatrix();
		glPopMatrix();
	}
}