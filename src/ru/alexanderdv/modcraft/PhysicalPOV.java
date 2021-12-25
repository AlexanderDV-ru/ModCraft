package ru.alexanderdv.modcraft;

import java.util.Arrays;

import ru.alexanderdv.modcraft.Block.Side;
import ru.alexanderdv.utils.VectorD;

public class PhysicalPOV extends POV {

	public static interface Collider { boolean hasCollisionAt(double[] position); }

	public static interface PhysicalEnviroment { double getGravity(); }

	protected VectorD velocity = new VectorD(position.size()), volution = new VectorD(rotation.size());
	protected VectorD velocityIncreasing = new VectorD(velocity.size()), volutionIncreasing = new VectorD(volution.size());
	protected VectorD size = new VectorD(position.size());

	public String getF3() {
		String f3 = "";
		f3 += "---Physical---" + "\n";
		f3 += "Location: " + Arrays.toString(position.coords) + "\n";
		f3 += "Rotation: " + Arrays.toString(rotation.coords) + "\n";
		f3 += "Velocity: " + Arrays.toString(velocity.coords) + "\n";
		f3 += "Volution: " + Arrays.toString(volution.coords) + "\n";
		f3 += "Size: " + Arrays.toString(size.coords) + "\n";
		return f3;
	}

	public Side isLooking() { return rotation.getX() > 120 && rotation.getX() < 270 ? Side.BACK : Side.FORWARD; }

	public VectorD getLookDir() {
		return new VectorD(new double[] {

				-Math.sin(Math.toRadians(rotation.getY())),

				-Math.max(Math.min(isLooking() == Side.BACK ? Math.tan(Math.toRadians(180 - rotation.getX())) : Math.tan(Math.toRadians(rotation.getX())), 1), -1),

				+Math.cos(Math.toRadians(rotation.getY())),

				0 });
	}

	protected double inertia = 10, kinematics = 5;

	public void applyVelocityIncreasing(double[] velocity, double[] adding, double ticksPerSecond) { for (int i = 0; i < adding.length; i++) { velocity[i] += adding[i] / ticksPerSecond; } }

	public void clearVelocityIncreasing(double[] velocity, double[] adding) { for (int i = 0; i < adding.length; i++) { adding[i] = 0; } }

	public void physics(PhysicalEnviroment enviroment) {
		velocityIncreasing.coords[1] -= enviroment.getGravity() * (0 + notInGroundTime);
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

		public boolean check(double[] coords, double[] size, Collider... colliders) { return check(coords, size, new int[0], colliders); }

		public boolean check(double[] coords, double[] size, int[] resursion, Collider... colliders) {
			for (int i = 0; i < coords.length; i++) {
				if (resursion.length > 0 && resursion[0] == i)
					continue;
				coords[i] += size[i];
				boolean plusCheck = resursion.length > 0 ? check(coords, colliders) : check(coords, size, new int[] { i }, colliders);
				coords[i] -= size[i];
				if (plusCheck)
					return true;
				coords[i] -= size[i];
				boolean minusCheck = resursion.length > 0 ? check(coords, colliders) : check(coords, size, new int[] { i }, colliders);
				coords[i] += size[i];
				if (minusCheck)
					return true;
			}
			return check(coords, colliders);
		}
	}

	public Collision collision = new Collision((coords, colliders) -> {
		for (Collider collider : colliders)
			if (collider.hasCollisionAt(coords))
				return true;
		return false;
	});

	boolean collisionsInsideColliders;
	double onCollisionMotionModifier, onCollisionVelocityModifier;

	protected void move(double motion, double[] coords, double[] velocity, int axis) {
		coords[axis] += motion;
		velocity[axis] -= motion;
	}

	public void velocityMotionWithInertia(double[] coords, double[] velocity, double inertia, Collider... colliders) {
		for (int axis = 0; axis < coords.length; axis++)
			for (double i = 0; i < Math.min(Math.abs(velocity[axis] / inertia), 100); i++) {
				double motion = i + 1 < Math.min(Math.abs(velocity[axis] / inertia), 100) ? (velocity[axis] / inertia < 0 ? -1 : 1) : (velocity[axis] / inertia) % 1;

				boolean hasCollisionBefore = collision.check(coords, size.coords, colliders);

				move(+motion, coords, velocity, axis);
				boolean hasCollisionAfter = collision.check(coords, size.coords, colliders);
				move(-motion, coords, velocity, axis);

				boolean hasCollision = hasCollisionAfter && (collisionsInsideColliders ? true : !hasCollisionBefore || axis == 1);
				// TODO ||axis==1 remove this crutch and make preventing of block placing if it
				// will make collision

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
