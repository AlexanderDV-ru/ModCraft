package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glScaled;

import java.util.Arrays;

import ru.alexanderdv.modcraft.Block.Side;
import ru.alexanderdv.modcraft.Input.DisplayInput;
import ru.alexanderdv.modcraft.Input.Key;
import ru.alexanderdv.modcraft.configs.SConfig;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.Named;
import ru.alexanderdv.utils.VectorD;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public class Controller extends PhysicalPOV implements Named, VerticalNormalised {
	private final String name;

	@Override
	public String getName() { return this.name; }

	public Controller(String name) { this.name = name; }

	public String getF3() { return "Name: " + name + "\n" + "Vision: " + Arrays.toString(vision.coords) + "\n" + "BreakDistance: " + Arrays.toString(breakDistance.coords) + "\n" + super.getF3(); }

	public double jump = 20, sprint = 3;
	public double speed = 5/* meters in second */, sensitivity = 1;
	public boolean sprinting;
	public boolean[] canMoveTo = new boolean[position.size()], moveAtLook = new boolean[position.size()];
	public VectorD vision = new VectorD(position.size());
	public VectorD breakDistance = new VectorD(position.size());
	public int idInHand;
	public double blocksInSecond = 1;

	public boolean isFlying() { return canMoveTo[1]; }

	public void setFlying(boolean flying) { this.canMoveTo[1] = flying; }

	public boolean isEngah() { return canMoveTo[3]; }

	public void setEngah(boolean engah) { this.canMoveTo[3] = engah; }

	@Override
	public void physics(PhysicalEnviroment enviroment) {
		if (!isFlying())
			super.physics(enviroment);
	}

	public static class UserController extends Controller {
		public UserController(String name) { super(name); }

		public DisplayInput input;
		public SConfig controls;

		/** Is setting from PlayerConfig */
		public boolean lineSelector, blockSelectorOff, onPlayerFixedSelector, blinkingSelector, transperantBlocksFromOtherWorlds;
		public Block selector;
		public double tntExplosionRadius;
		public String canBreak, canBreakThrough;

		public String selector(World world, double ticksPerSecond, boolean renderMode) {
			VectorD lookDir = getLookDir();
			glColor4f(0, 0, 0, 1);
			for (int m = 0; m < MathUtils.max(breakDistance.coords); m += 1) {
				for (int i = 0; i < breakDistance.coords.length; i++)
					if (lookDir.coords[0] * m > breakDistance.coords[0])
						return "";
				double x = position.getX() + lookDir.getX() * m;
				double y = position.getY() + lookDir.getY() * m + 0.05;
				double z = position.getZ() + lookDir.getZ() * m;
				double w = position.getW();
				if (canBreak(world.getBlock(x, y, z, w))) {
					if (renderMode) {
						if (lineSelector) {
							glTranslated(x, y - 0.1, z);
							glBegin(GL_LINES);
							glVertex3d(lookDir.getX() * 0, lookDir.getY() * 0, lookDir.getZ() * 0);
							glVertex3d(lookDir.getX() * 1, lookDir.getY() * 1, lookDir.getZ() * 1);
							glEnd();
							glTranslated(-x, -y + 0.1, -z);
						}
						if (!blockSelectorOff) {
							if (onPlayerFixedSelector)
								glTranslated(x, y, z);
							else glTranslated((int) x, (int) y, (int) z);
							if (!blinkingSelector) {
								glScaled(1.01, 1.01, 1.01);
								glTranslated(-.005, -.005, -.005);
							}
							selector.render(new boolean[6]);
							if (!blinkingSelector) {
								glScaled(1 / 1.01, 1 / 1.01, 1 / 1.01);
								glTranslated(.005, .005, .005);
							}
							if (onPlayerFixedSelector)
								glTranslated(-x, -y, -z);
							else glTranslated((int) -x, (int) -y, (int) -z);
							return "";
						}
					} else if (breakTime > ticksPerSecond / blocksInSecond) {
						if (input.isButtonDown(0)) {
							breakTime = 0;
							return "setblock " + x + " " + y + " " + z + " " + w + " " + 0;
						}
						if (input.isButtonDown(1)) {
							breakTime = 0;
							return rightButtonDown(world, x, y, z, w, lookDir);
						}
						if (input.isButtonDown(2)) {
							breakTime = 0;
							idInHand = world.getBlock(x, y, z, w).id;
							return "getblock " + x + " " + y + " " + z + " " + w;
						}
					}
				} else if (!canBreakThrough(world.getBlock(x, y, z, w)))
					return "";
			}
			breakTime++;
			return "";
		}

		public String rightButtonDown(World world, double x, double y, double z, double w, VectorD lookDir) {
			if (world.getBlock(x, y, z, w).getName().contains("tnt"))
				return "explosion " + x + " " + y + " " + z + " " + w + " " + tntExplosionRadius;
			else return "setblock " + (x - lookDir.getX()) + " " + (y - lookDir.getY()) + " " + (z - lookDir.getZ()) + " " + (w - lookDir.getW()) + " " + idInHand;
		}

		public boolean canBreak(Block block) {
			String canBreak = "," + this.canBreak + ",";

			if ((canBreak.contains(",all,") || canBreak.contains(",*,")) && !(canBreak.contains(",!" + block.getName() + ",") || canBreak.contains(",!" + block.id + ",")) && block.id != 0)
				return true;
			if (block.isBreakable() && canBreak.contains(",default,") && !(canBreak.contains(",!" + block.getName() + ",") || canBreak.contains(",!" + block.id + ",")) && block.id != 0)
				return true;
			if (canBreak.contains("," + block.getName() + ",") || canBreak.contains("," + block.id + ","))
				return true;
			return false;
		}

		public boolean canBreakThrough(Block block) {
			String through = "," + this.canBreakThrough + ",";

			if (block.id == 0)
				return true;
			if ((through.contains(",all,") || through.contains(",*,")) && !(through.contains(",!" + block.getName() + ",") || through.contains(",!" + block.id + ",")))
				return true;
			if (!block.isBreakable() && through.contains(",default,") && !(through.contains(",!" + block.getName() + ",") || through.contains(",!" + block.id + ",")))
				return true;
			if (through.contains("," + block.getName() + ",") || through.contains("," + block.id + ","))
				return true;
			return false;
		}

		public double breakTime;

		public boolean escape = true, ended;

		public void controls() {
			if (controls.getInputValue(input, "after", "endprogram").coords[0] == 1)
				ended = true;
			for (Key input : this.input.nextKeys)
				if (controls.getInputValue(input, "switch", "escape").coords[0] == 1) {
					escape = !escape;
					this.input.setGrabbed(!escape);
				}
			if (isInMenu())
				return;
			for (Key input : this.input.nextKeys) {
				if (controls.getInputValue(input, "switch", "fly").coords[0] == 1) {
					setFlying(!isFlying());
					notInGroundTime = 0;
				}
				if (controls.getInputValue(input, "switch", "engah").coords[0] == 1)
					setEngah(!isEngah());
				if (controls.getInputValue(input, "switch", "spectator").coords[0] == 1 || controls.getInputValue(input, "switch", "nocollision").coords[0] == 1)
					collision.disabled = !collision.disabled;

				if (controls.getInputValue(input, "click", "jump").coords[0] == 1) {
					notInGroundTime = 0;
					velocityIncreasing.coords[1] += jump * (!isFlying() ? 1 : 0);
				}
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

			sprinting = controls.getInputValue(input, "when", "sprint").coords[0] == 1;// it doesn't work if user have old custom witout shift
			// TODO make stacking loading of all files, not only top?

			VectorD keysMove = controls.getInputValue(input, "when", "4_axis");

			VectorD motionByLookDir = new VectorD(new double[] {

					(+keysMove.getX() * -getLookDir().getZ()) + keysMove.getZ() * (isLooking() == Side.BACK && isFreecam() ? -1 : 1) * getLookDir().getX(),

					(+keysMove.getY() + keysMove.getZ() * getLookDir().getY() * (isFreecam() ? 1 : 0)),

					(+keysMove.getZ() * (isLooking() == Side.BACK && isFreecam() ? -1 : 1) * +getLookDir().getZ()) + keysMove.getX() * getLookDir().getX() });

			for (int axis = 0; axis < velocityIncreasing.coords.length; axis++)
				velocityIncreasing.coords[axis] += (!canMoveTo[axis] ? 0 : (moveAtLook[axis] ? motionByLookDir.coords[axis] : keysMove.coords[axis])) * this.speed * (sprinting ? sprint : 1);

			volutionIncreasing.coords[0] += input.getVolutionX() * this.sensitivity;
			volutionIncreasing.coords[1] += input.getVolutionY() * this.sensitivity;
		}

		public boolean freecamOnFlying, freecamOnSpectator;

		public boolean isFreecam() { return freecamOnFlying && moveAtLook[1] || freecamOnSpectator && collision.disabled; }

		public boolean isInMenu() { return escape; }

		public VectorD renderWorldSize, count, modifier;

		public void selectRenderDirectionByRotation(World world) {
			if (renderWorldSize == null)
				renderWorldSize = new VectorD(world.size.size());
			if (count == null)
				count = new VectorD(world.size.size());
			if (modifier == null)
				modifier = new VectorD(world.size.size());

			for (int i = 0; i < position.coords.length; i++) {
				// if (rotation.getY() > 0 && rotation.getY() < 180 && i == 0 || rotation.getX()
				// < 180 && i == 1 || rotation.getY() > 90 && rotation.getY() < 270 && i == 2) {
				renderWorldSize.coords[i] = (position.coords[i] - vision.coords[i]);
				count.coords[i] = position.coords[i] + vision.coords[i];
				modifier.coords[i] = -1;
				// }
				// if (rotation.getY() > 180 && rotation.getY() < 360 && i == 0 ||
				// rotation.getX() > 180 && i == 1 || (rotation.getY() > 270 || rotation.getY()
				// < 90) && i == 2) {
				// renderWorldSize.coords[i] = position.coords[i] + vision.coords[i] - 1;
				// count.coords[i] = (position.coords[i] - vision.coords[i]) - 1;
				// modifier.coords[i] = 1;
				// }
				if (!world.repeat) {
					renderWorldSize.coords[i] = MathUtils.clampD(renderWorldSize.coords[i], 0, world.size.coords[i]);
					count.coords[i] = MathUtils.clampD(count.coords[i], 0, world.size.coords[i]);
				}
			}
		}

		public static interface ForEachPlace { void doForPlace(double x, double y, double z, double w); }

		public void doForEachSeenBlock(ForEachPlace operation) {
			for (double x = renderWorldSize.coords[0]; x * modifier.coords[0] > count.coords[0] * modifier.coords[0]; x -= modifier.coords[0])
				for (double y = renderWorldSize.coords[1]; y * modifier.coords[1] > count.coords[1] * modifier.coords[1]; y -= modifier.coords[1])
					for (double z = renderWorldSize.coords[2]; z * modifier.coords[2] > count.coords[2] * modifier.coords[2]; z -= modifier.coords[2])
						for (double w = renderWorldSize.coords[3]; w * modifier.coords[3] > count.coords[3] * modifier.coords[3]; w -= modifier.coords[3])
							operation.doForPlace(x, y, z, w);
		}
	}
}