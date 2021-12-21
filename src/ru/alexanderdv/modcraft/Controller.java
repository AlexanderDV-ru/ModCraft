package ru.alexanderdv.modcraft;

import ru.alexanderdv.utils.Named;

public class Controller extends PhysicalPOV implements Named {
	public final String name;

	@Override
	public String getName() { return this.name; }

	public Controller(String name) { this.name = name; }

	public void applyAdding(double[] velocity, double[] adding, double speed) {
		for (int i = 0; i < adding.length; i++) {
			velocity[i] += adding[i] * speed;
			adding[i] = 0;
		}
	}

	protected double jump = 20, sprint = 3;
	protected double speed = 1, sensitivity = 1;
	protected boolean flying = true, sprinting;

	@Override
	public void applyPhysicalEnviroment(PhysicalEnviroment enviroment) {
		if (!flying)
			super.applyPhysicalEnviroment(enviroment);
	}

	public static class UserController extends Controller {
		public UserController(String name) { super(name); }

		DisplayTabWindow display;
		Input input;

		public double getInertia() { return inertia * input.keyboardTune; }

		public double getKinematics() { return kinematics * input.mouseTune; }

		public Custom custom;
		boolean escape = true, ended;

		public void controls() {
			if (custom.isControl(input, "jet", "endprogram"))
				ended = true;
			while (input.next()) {// TODO make full interception of Keyboard by input, change to input.getNexts()
				if (custom.isControl(input, "switch", "escape")) {
					input.setCursorPosition(display.getWidth() / 2, display.getHeight() / 2);
					escape = !escape;
					input.setGrabbed(!escape);
				}
				if (isInMenu())
					continue;
				if (custom.isControl(input, "switch", "fly"))
					flying = !flying;
				if (custom.isControl(input, "switch", "spectator||nocollision"))// TODO fix bug in comments in cfg regex, it can't work with // and # comments
					preventMotionOnCollision = clearVelocityOnCollision = !clearVelocityOnCollision;

				if (custom.isControl(input, "click", "sneak"))
					velocityAdding[1] += 1 * (!flying ? 1 : 0);
				applyAdding(velocity, velocityAdding, jump * input.keyboardTune);
			}
			if (isInMenu())
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

			sprinting = custom.isControl(input, "when", "sprint");// it doesn't work if user have old custom witout shift
			// TODO make stacking loading of all files, not only top?

			double[] axis = custom.getAxis(input, 4).coords;
			double sin = Math.sin(Math.toRadians(rotation.coords[1]));
			double cos = Math.cos(Math.toRadians(rotation.coords[1]));

			velocityAdding[0] += (-axis[0] * cos - axis[2] * sin);
			velocityAdding[1] += (+axis[1] * +1 - +axis[1] * +-0) * (flying ? 1 : 0);
			velocityAdding[2] += (+axis[2] * cos - axis[0] * sin);

			volutionAdding[1] += input.getDX();
			volutionAdding[0] -= input.getDY();

			applyAdding(velocity, velocityAdding, this.speed * (sprinting ? sprint : 1) * input.keyboardTune);
			applyAdding(volution, volutionAdding, this.sensitivity * input.mouseTune);
		}

		public boolean isInMenu() { return escape; }

		int[] size = new int[3];
		int xCount = 0, yCount = 0, zCount = 0;
		int xModifier = 0, yModifier = 0, zModifier = 0;

		public void selectRenderDirectionByRotation(World world) {
			if (rotation.coords[1] > 0 && rotation.coords[1] < 180) {
				size[0] = world.size[0] - 1;
				xCount = -1;
				xModifier = 1;
			}
			if (rotation.coords[1] > 180 && rotation.coords[1] < 360) {
				size[0] = 0;
				xCount = world.size[0];
				xModifier = -1;
			}
			if (rotation.coords[1] > 90 && rotation.coords[1] < 270) {
				size[2] = world.size[2] - 1;
				zCount = -1;
				zModifier = 1;
			}
			if (rotation.coords[1] > 270 || rotation.coords[1] < 90) {
				size[2] = 0;
				zCount = world.size[2];
				zModifier = -1;
			}
			if (rotation.coords[0] > 180) {
				size[1] = world.size[1] - 1;
				yCount = -1;
				yModifier = 1;
			}
			if (rotation.coords[0] < 180) {
				size[1] = 0;
				yCount = world.size[1];
				yModifier = -1;
			}
		}

		public static interface ForEachPlace { void doForPlace(int x, int y, int z); }

		public void doForEachSeenBlock(ForEachPlace operation) {
			for (int x = size[0]; x * xModifier > xCount * xModifier; x -= xModifier)
				for (int y = size[1]; y * yModifier > yCount * yModifier; y -= yModifier)
					for (int z = size[2]; z * zModifier > zCount * zModifier; z -= zModifier)
						operation.doForPlace(x, y, z);
		}
	}
}