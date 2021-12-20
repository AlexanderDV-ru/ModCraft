package ru.alexanderdv.modcraft;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public class Controller extends POV {
	boolean escape = true, fly = true, ended;
	protected double[] deltas = { 0, 0, 0 };
	float senvisity = 100, kinematics = 1f, speed = 1.5f, airSlowing = 1.5f;

	public void controls(DisplayTabWindow display, Input input) {
		if (Keyboard.isKeyDown(Keyboard.KEY_END))
			ended = true;
		while (Keyboard.next()) {
			if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
				Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
				escape = !escape;
				Mouse.setGrabbed(!escape);
			}
			if (escape)
				continue;
			if (Keyboard.isKeyDown(Keyboard.KEY_F))
				fly = !fly;
			if (!fly)
				if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))
					deltas[1] += 50;
		}
		if (escape)
			return;

		double za = ((Keyboard.isKeyDown(Keyboard.KEY_W) ? 1 : 0) - (Keyboard.isKeyDown(Keyboard.KEY_S) ? 1 : 0));
		double xa = ((Keyboard.isKeyDown(Keyboard.KEY_A) ? 1 : 0) - (Keyboard.isKeyDown(Keyboard.KEY_D) ? 1 : 0));
		double sin = Math.sin(Math.toRadians(rotation.coords[1]));
		double cos = Math.cos(Math.toRadians(rotation.coords[1]));
		deltas[0] += xa * cos - za * sin;
		deltas[2] += za * cos + xa * sin;
		if (fly) {
			if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))
				deltas[1]++;
			if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
				deltas[1]--;
		} else deltas[1]--;

		rotation.coords[1] += ((float) Mouse.getDX()) / 2000 * senvisity;
		rotation.coords[0] -= ((float) Mouse.getDY()) / 2000 * senvisity;

		double finalSpeed = speed / airSlowing, tunedInertia = kinematics * 10 / finalSpeed;
		for (int c = 0; c < coords.length; c++) {
			inertia[c].setCoordDelta(coords[c], deltas[c]);
			inertia[c].setSpeedInertia(finalSpeed, tunedInertia);
			inertia[c].calculate();
			coords[c] = inertia[c].getCoord();
			deltas[c] = inertia[c].getDelta();
		}
	}

	Inertia[] inertia = new Inertia[] { new Inertia(), new Inertia(), new Inertia() };

	int xSize = 0, ySize = 0, zSize = 0;
	int xCount = 0, yCount = 0, zCount = 0;
	int xModifier = 0, yModifier = 0, zModifier = 0;

	public void selectRenderDirectionByRotation(World world) {
		if (rotation.coords[1] > 0 && rotation.coords[1] < 180) {
			xSize = world.xSize - 1;
			xCount = -1;
			xModifier = 1;
		}
		if (rotation.coords[1] > 180 && rotation.coords[1] < 360) {
			xSize = 0;
			xCount = world.xSize;
			xModifier = -1;
		}

		if (rotation.coords[1] > 90 && rotation.coords[1] < 270) {
			zSize = world.zSize - 1;
			zCount = -1;
			zModifier = 1;
		}
		if (rotation.coords[1] > 270 || rotation.coords[1] < 90) {
			zSize = 0;
			zCount = world.zSize;
			zModifier = -1;
		}
		if (rotation.coords[0] > 180) {
			ySize = world.ySize - 1;
			yCount = -1;
			yModifier = 1;
		}
		if (rotation.coords[0] < 180) {
			ySize = 0;
			yCount = world.ySize;
			yModifier = -1;
		}
	}

	public static interface ForEachPlace { void doForPlace(int x, int y, int z); }

	public void doForEachBlockInFieldOfVision(ForEachPlace operation) {
		for (int x = xSize; x * xModifier > xCount * xModifier; x -= xModifier)
			for (int y = ySize; y * yModifier > yCount * yModifier; y -= yModifier)
				for (int z = zSize; z * zModifier > zCount * zModifier; z -= zModifier)
					operation.doForPlace(x, y, z);
	}
}