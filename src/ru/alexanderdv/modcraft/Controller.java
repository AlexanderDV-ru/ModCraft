package ru.alexanderdv.modcraft;

import ru.alexanderdv.modcraft.Input.DisplayInput;
import ru.alexanderdv.modcraft.Input.Key;
import ru.alexanderdv.utils.Named;

public class Controller extends PhysicalPOV implements Named {
	public final String name;

	@Override
	public String getName() { return this.name; }

	public Controller(String name) { this.name = name; }

	public String getF3() { return "Name: " + name + "\n" + super.getF3(); }

	protected double jump = 20, sprint = 3;
	protected double speed = 1, sensitivity = 1;
	protected boolean flying = true, sprinting;

	@Override
	public void physics(PhysicalEnviroment enviroment) {
		if (!flying)
			super.physics(enviroment);
	}

	public static class UserController extends Controller {
		public UserController(String name) { super(name); }

		DisplayShell display;
		DisplayInput input;

		public double getInertia() { return inertia; }

		public double getKinematics() { return kinematics; }

		public Custom custom;
		boolean escape = true, ended;

		public void controls() {
			if (custom.getInputValue(input, "after", "endprogram").coords[0] == 1)
				ended = true;
			for (Key input : this.input.nextKeys)
				if (custom.getInputValue(input, "switch", "escape").coords[0] == 1) {
					this.input.setCursorPosition(display.getWidth() / 2, display.getHeight() / 2);
					escape = !escape;
					this.input.setGrabbed(!escape);
				}
			if (isInMenu())
				return;
			for (Key input : this.input.nextKeys) {
				if (custom.getInputValue(input, "switch", "fly").coords[0] == 1) {
					flying = !flying;
					notInGroundTime = 0;
				}
				if (custom.getInputValue(input, "switch", "spectator").coords[0] == 1 || custom.getInputValue(input, "switch", "nocollision").coords[0] == 1)
					collision.disabled = !collision.disabled;

				if (custom.getInputValue(input, "click", "sneak").coords[0] == 1)
					velocityIncreasing[1] += jump * (!flying ? 1 : 0);
			}
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

			sprinting = custom.getInputValue(input, "when", "sprint").coords[0] == 1;// it doesn't work if user have old custom witout shift
			// TODO make stacking loading of all files, not only top?

			double[] axes = custom.getInputValue(input, "when", "4_axis").coords;
			double sin = Math.sin(Math.toRadians(rotation.coords[1]));
			double coef = 1;
			double cos = Math.cos(Math.toRadians(rotation.coords[1]));

			velocityIncreasing[0] += (-axes[0] * cos - axes[2] * sin) * this.speed * (sprinting ? sprint : 1);
			velocityIncreasing[1] += (+axes[1] * (flying ? coef : 0)) * this.speed * (sprinting ? sprint : 1);
			velocityIncreasing[2] += (+axes[2] * cos - axes[0] * sin) * this.speed * (sprinting ? sprint : 1);

			volutionIncreasing[0] += input.getVolutionX() * this.sensitivity;
			volutionIncreasing[1] += input.getVolutionY() * this.sensitivity;
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