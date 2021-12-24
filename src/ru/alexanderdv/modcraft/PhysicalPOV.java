package ru.alexanderdv.modcraft;

import java.util.Arrays;

import ru.alexanderdv.modcraft.Block.Side;
import ru.alexanderdv.modcraft.Config.SConfig;
import ru.alexanderdv.utils.VectorD;

public class PhysicalPOV extends POV {

	public static interface Collider { boolean hasCollisionAt(double[] position); }

	public static interface PhysicalEnviroment { double getGravity(); }

	protected double[] velocity = { 0, 0, 0 }, volution = { 0, 0 };
	protected double[] velocityIncreasing = { 0, 0, 0 }, volutionIncreasing = { 0, 0 };

	public String getF3() {
		String f3 = "";
		f3 += "---Physical---" + "\n";
		f3 += "Location: " + Arrays.toString(position.coords) + "\n";
		f3 += "Rotation: " + Arrays.toString(rotation.coords) + "\n";
		f3 += "Velocity: " + Arrays.toString(velocity) + "\n";
		f3 += "Volution: " + Arrays.toString(volution) + "\n";
		return f3;
	}

	public Side isLooking() { return rotation.getX() > 120 && rotation.getX() < 270 ? Side.BACK : Side.FORWARD; }

	public VectorD getLookDir() {
		return new VectorD(new double[] {

				-Math.sin(Math.toRadians(rotation.getY())),

				-Math.max(Math.min(isLooking() == Side.BACK ? Math.tan(Math.toRadians(180 - rotation.getX())) : Math.tan(Math.toRadians(rotation.getX())), 1), -1),

				+Math.cos(Math.toRadians(rotation.getY())) });
	}

	protected double inertia = 10, kinematics = 5;

	public void applyVelocityIncreasing(double[] velocity, double[] adding, double ticksPerSecond) { for (int i = 0; i < adding.length; i++) { velocity[i] += adding[i] / ticksPerSecond; } }

	public void clearVelocityIncreasing(double[] velocity, double[] adding) { for (int i = 0; i < adding.length; i++) { adding[i] = 0; } }

	public void physics(PhysicalEnviroment enviroment) {
		velocityIncreasing[1] -= enviroment.getGravity() * (1 + notInGroundTime);
		// TODO wind?
	}

	public static interface CollisionChecker { public boolean check(double[] coords, Collider... colliders); }

	public static class Collision {
		public boolean disabled;
		CollisionChecker checker;

		public Collision(CollisionChecker checker) {
			super();
			this.checker = checker;
		}

		public boolean check(double[] coords, Collider... colliders) {
			if (disabled)
				return false;
			return checker.check(coords, colliders);
		}
	}

	public Collision collision = new Collision((coords, colliders) -> {
		for (Collider collider : colliders)
			if (collider.hasCollisionAt(coords))
				return true;
		return false;
	});
	public SConfig misc = new SConfig("configs/misc.cfg");

	boolean collisionsInsideColliders = misc.bool("collisionsInsideColliders");
	double onCollisionMotionModifier = misc.num("onCollisionMotionModifier"), onCollisionVelocityModifier = misc.num("onCollisionVelocityModifier");

	protected void move(double motion, double[] coords, double[] velocity, int axis) {
		coords[axis] += motion;
		velocity[axis] -= motion;
	}

	public void velocityMotionWithInertia(double[] coords, double[] velocity, double inertia, Collider... colliders) {
		for (int axis = 0; axis < coords.length; axis++)
			for (double i = 0; i < Math.min(Math.abs(velocity[axis] / inertia), 100); i++) {
				double motion = i + 1 < Math.min(Math.abs(velocity[axis] / inertia), 100) ? (velocity[axis] / inertia < 0 ? -1 : 1) : (velocity[axis] / inertia) % 1;

				boolean hasCollisionBefore = collision.check(coords, colliders);

				move(+motion, coords, velocity, axis);
				boolean hasCollisionAfter = collision.check(coords, colliders);
				move(-motion, coords, velocity, axis);

				boolean hasCollision = hasCollisionAfter && (collisionsInsideColliders ? true : !hasCollisionBefore);

				if (hasCollision)
					motion *= onCollisionMotionModifier;
				move(motion, coords, velocity, axis);
				if (hasCollision)
					velocity[axis] *= onCollisionVelocityModifier;

				if (axis == 1 && !hasCollision && i == 0)
					notInGroundTime++;
			}
	}

	double notInGroundTime;

	public double getInertia() { return inertia; }

	public double getKinematics() { return kinematics; }
}
