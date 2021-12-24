package ru.alexanderdv.modcraft;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glScaled;

import ru.alexanderdv.modcraft.Block.Side;
import ru.alexanderdv.modcraft.Config.SConfig;
import ru.alexanderdv.modcraft.Input.DisplayInput;
import ru.alexanderdv.modcraft.Input.Key;
import ru.alexanderdv.utils.Named;
import ru.alexanderdv.utils.VectorD;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public class Controller extends PhysicalPOV implements Named, VerticalNormalised {
	public final String name;

	@Override
	public String getName() { return this.name; }

	public Controller(String name) { this.name = name; }

	public String getF3() { return "Name: " + name + "\n" + super.getF3(); }

	protected double jump = misc.bool("jump") ? misc.num("jump") : 20, sprint = misc.bool("sprint") ? misc.num("sprint") : 3;
	protected double speed = misc.bool("speed") ? misc.num("speed") : 5/* metes in second */, sensitivity = misc.bool("sensitivity") ? misc.num("sensitivity") : 1;
	protected boolean sprinting;
	boolean[] canMoveTo = { true, false, true }, moveAtLook = { true, true, true };
	double blockBreakingDistance = misc.bool("blockBreakingDistance") ? misc.num("blockBreakingDistance") : 5;

	public boolean isFlying() { return canMoveTo[1]; }

	public void setFlying(boolean flying) { this.canMoveTo[1] = flying; }

	@Override
	public void physics(PhysicalEnviroment enviroment) {
		if (!isFlying())
			super.physics(enviroment);
	}

	public static class UserController extends Controller {
		public UserController(String name) { super(name); }

		public DisplayInput input;
		public SConfig controls, misc = new SConfig("configs/misc.cfg");

		boolean canBreakAll = misc.bool("canBreakAll");
		boolean lineSelector = misc.bool("lineSelector"), blockSelectorOff = misc.bool("blockSelectorOff");
		boolean onPlayerFixedSelector = misc.bool("onPlayerFixedSelector");
		boolean blinkingSelector = misc.bool("blinkingSelector");
		Block selector = new Block(0, 0, 0, null, (int) (misc.bool("selectorId") ? misc.num("selectorId") : 21));

		public void createExplosion(World world, double ex, double ey, double ez, int r) {
			for (int x = (int) (ex - r); x < ex + r; x++)
				for (int y = (int) (ey - r); y < ey + r; y++)
					for (int z = (int) (ez - r); z < ez + r; z++)
						if (Math.sqrt(Math.pow(ex - x, 2) + Math.pow(ey - y, 2) + Math.pow(ez - z, 2)) < r)
							world.setBlock(x, y, z, 0);
		}

		int idInHand;
		double blocksInSecond = misc.bool("blocksInSecond") ? misc.num("blocksInSecond") : 1, breakTime;
		double tntExplosionRadius = misc.bool("tntExplosionRadius") ? misc.num("tntExplosionRadius") : 5;

		public void selector(World world, double ticksPerSecond, boolean renderMode) {
			glColor4f(0, 0, 0, 1);
			for (int m = 0; m < blockBreakingDistance; m++) {
				double x = position.getX() + getLookDir().getX() * m;
				double y = position.getY() + getLookDir().getY() * m + 0.05;
				double z = position.getZ() + getLookDir().getZ() * m;
				if ((world.getBlock((int) x, (int) y, (int) z).isBreakable() || canBreakAll && world.getBlock((int) x, (int) y, (int) z).id != 0)) {
					if (renderMode) {
						if (lineSelector) {
							glTranslated(x, y - 0.1, z);
							glBegin(GL_LINES);
							glVertex3d(getLookDir().getX() * 0, getLookDir().getY() * 0, getLookDir().getZ() * 0);
							glVertex3d(getLookDir().getX() * 1, getLookDir().getY() * 1, getLookDir().getZ() * 1);
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
						}
					} else if (breakTime > ticksPerSecond / blocksInSecond) {
						if (input.isButtonDown(0)) {
							world.setBlock((int) x, (int) y, (int) z, 0);
							breakTime = 0;
							return;
						}
						if (input.isButtonDown(1)) {
							if (world.getBlock((int) x, (int) y, (int) z).getName().contains("tnt"))
								createExplosion(world, x, y, z, (int) tntExplosionRadius);
							else world.setBlock((int) (x - getLookDir().getX()), (int) (y - getLookDir().getY()), (int) (z - getLookDir().getZ()), idInHand);
							breakTime = 0;
							return;
						}
						if (input.isButtonDown(2)) {
							idInHand = world.getBlock((int) x, (int) y, (int) z).id;
							breakTime = 0;
							return;
						}
					}
				}
			}
			breakTime++;
		}

		boolean escape = true, ended;

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
				if (controls.getInputValue(input, "switch", "spectator").coords[0] == 1 || controls.getInputValue(input, "switch", "nocollision").coords[0] == 1)
					collision.disabled = !collision.disabled;

				if (controls.getInputValue(input, "click", "sneak").coords[0] == 1)
					velocityIncreasing[1] += jump * (!isFlying() ? 1 : 0);
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

			for (int axis = 0; axis < velocityIncreasing.length; axis++)
				velocityIncreasing[axis] += (!canMoveTo[axis] ? 0 : (moveAtLook[axis] ? motionByLookDir.coords[axis] : keysMove.coords[axis])) * this.speed * (sprinting ? sprint : 1);

			volutionIncreasing[0] += input.getVolutionX() * this.sensitivity;
			volutionIncreasing[1] += input.getVolutionY() * this.sensitivity;
		}

		boolean freecamOnFlying = misc.bool("freecamOnFlying"), freecamOnSpectator = !misc.bool("dontFreecamOnSpectator");

		public boolean isFreecam() { return freecamOnFlying && moveAtLook[1] || freecamOnSpectator && collision.disabled; }

		public boolean isInMenu() { return escape; }

		int[] size = new int[3];
		int xCount = 0, yCount = 0, zCount = 0;
		int xModifier = 0, yModifier = 0, zModifier = 0;

		public void selectRenderDirectionByRotation(World world) {
			if (rotation.getY() > 0 && rotation.getY() < 180) {
				size[0] = 0;
				xCount = world.size[0];
				xModifier = -1;
			}
			if (rotation.getY() > 180 && rotation.getY() < 360) {
				size[0] = world.size[0] - 1;
				xCount = -1;
				xModifier = 1;
			}
			if (rotation.getY() > 90 && rotation.getY() < 270) {
				size[2] = 0;
				zCount = world.size[2];
				zModifier = -1;
			}
			if (rotation.getY() > 270 || rotation.getY() < 90) {
				size[2] = world.size[2] - 1;
				zCount = -1;
				zModifier = 1;
			}
			if (rotation.getX() > 180) {
				size[1] = world.size[1] - 1;
				yCount = -1;
				yModifier = 1;
			}
			if (rotation.getX() < 180) {
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