package ru.alexanderdv.modcraft;

public class Controller extends POV {
	boolean escape = true, fly = true, ended;
	protected double[] deltas = { 0, 0, 0 };
	float senvisity = 100, kinematics = 1f, speed = 1.5f, airSlowing = 1.5f;

	public void controls(DisplayTabWindow display, Input input) {
		if (input.isKeyDown(input.KEY_END))
			ended = true;
		while (input.next()) {
			if (input.isKeyDown(input.KEY_ESCAPE)) {
				input.setCursorPosition(display.getWidth() / 2, display.getHeight() / 2);
				escape = !escape;
				input.setGrabbed(!escape);
			}
			if (escape)
				continue;
			if (input.isKeyDown(input.KEY_F))
				fly = !fly;
			if (!fly)
				if (input.isKeyDown(input.KEY_SPACE))
					deltas[1] += 50;
		}
		if (escape)
			return;

		double za = ((input.isKeyDown(input.KEY_W) ? 1 : 0) - (input.isKeyDown(input.KEY_S) ? 1 : 0));
		double xa = ((input.isKeyDown(input.KEY_A) ? 1 : 0) - (input.isKeyDown(input.KEY_D) ? 1 : 0));
		double sin = Math.sin(Math.toRadians(rotation.coords[1]));
		double cos = Math.cos(Math.toRadians(rotation.coords[1]));
		deltas[0] += xa * cos - za * sin;
		deltas[2] += za * cos + xa * sin;
		if (fly) {
			if (input.isKeyDown(input.KEY_SPACE))
				deltas[1]++;
			if (input.isKeyDown(input.KEY_LCONTROL))
				deltas[1]--;
		} else deltas[1]--;

		rotation.coords[1] += ((float) input.getDX()) / 2000 * senvisity;
		rotation.coords[0] -= ((float) input.getDY()) / 2000 * senvisity;

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