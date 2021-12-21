package ru.alexanderdv.modcraft;

import ru.alexanderdv.utils.Named;

public class Controller extends POV implements Named {
	public final String name;

	public Controller(String name) { this.name = name; }

	@Override
	public String getName() { return this.name; }

	public Custom custom;
	boolean escape = true, fly = true, ended;
	protected double[] deltas = { 0, 0, 0 }, rotationsDeltas = { 0, 0 };
	double jump = 50, gravity = 9.8;
	double speed = 1, inertia = 10;
	double sensitivity = 1, kinematics = 10;// TODO 0.5 divide world picture to 2, mind this bug how non strict blocks

	public void controls(DisplayTabWindow display, Input input) {
		double keyboardTune = 1 / 5d, mouseTune = 1 / 10d;
		double speed = this.speed * keyboardTune, sensitivity = this.sensitivity * mouseTune;
		double inertia = this.inertia * keyboardTune, kinematics = this.kinematics * mouseTune;
		double gravity = this.inertia * keyboardTune, jump = this.kinematics * mouseTune;
		if (custom.isControl(input, "jet", "endprogram"))
			ended = true;
		while (input.next()) {
			if (custom.isControl(input, "switch", "escape")) {
				input.setCursorPosition(display.getWidth() / 2, display.getHeight() / 2);
				escape = !escape;
				input.setGrabbed(!escape);
			}
			if (escape)
				continue;
			if (custom.isControl(input, "switch", "fly"))
				fly = !fly;
			if (!fly)
				if (custom.isControl(input, "click", "jump"))
					deltas[1] += jump;
		}
		if (escape)
			return;
		// TODO change all physical system: add update adding to delta and in the end
		// multiply it to speed or sensitivity, change delta to velocity
		// TODO add shift sprinting
		// Decide between realistic physics model or many interface properties. I think,
		// that second, that's a game, not physical simulation. Hmm, but default value
		// isn't zero, its one, HERE no conflicts (what about other places?). Stop, but
		// if one is no kinematics, what is yes? and -1 is moving back?
		// Also decide between method realistic time simulation or physics, first is
		// more useful, you need do it only in main timer, but you will restricted by
		// default updates count.
		// TODO make better texture system
		// TODO make BlockId system with texture, color, model and other how parts of
		// it, and also dynamic coloring

		double[] axis = custom.getAxis(input, 4).coords;
		double sin = Math.sin(Math.toRadians(rotation.coords[1]));
		double cos = Math.cos(Math.toRadians(rotation.coords[1]));
		deltas[0] += (-axis[0] * cos - axis[2] * sin) * speed;
		deltas[1] += (+axis[1] * +1 - +axis[1] * +-0) * speed * (fly ? 1 : 0);
		deltas[2] += (+axis[2] * cos - axis[0] * sin) * speed;

		deltas[1] -= gravity * (fly ? 0 : 1);
		// TODO wind?

		rotationsDeltas[1] += input.getDX() * sensitivity;
		rotationsDeltas[0] -= input.getDY() * sensitivity;

		for (int c = 0; c < coords.length; c++) {
			coords[c] += deltas[c] / inertia;
			deltas[c] -= deltas[c] / inertia;
		}
		for (int c = 0; c < rotation.coords.length; c++) {
			rotation.coords[c] += rotationsDeltas[c] / kinematics;
			rotationsDeltas[c] -= rotationsDeltas[c] / kinematics;
		}
	}

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

	public void doForEachSeenBlock(ForEachPlace operation) {
		for (int x = xSize; x * xModifier > xCount * xModifier; x -= xModifier)
			for (int y = ySize; y * yModifier > yCount * yModifier; y -= yModifier)
				for (int z = zSize; z * zModifier > zCount * zModifier; z -= zModifier)
					operation.doForPlace(x, y, z);
	}
}