package ru.alexanderdv.modcraft;

public class PhysicalPOV extends POV {

	public static interface Collider { boolean hasCollisionAt(double[] position); }

	public static interface PhysicalEnviroment { double getGravity(); }

	protected double[] velocity = { 0, 0, 0 }, volution = { 0, 0 };
	protected double[] velocityAdding = { 0, 0, 0 }, volutionAdding = { 0, 0 };

	protected double inertia = 10, kinematics = 10;

	public void applyPhysicalEnviroment(PhysicalEnviroment enviroment) {
		velocity[1] -= enviroment.getGravity() / 5;
		// TODO wind?
	}

	public void applyMotion(Collider... colliders) {
		applyMotion(coords, velocity, getInertia(), colliders);
		applyMotion(rotation.coords, volution, getKinematics(), colliders);
	}

	boolean preventMotionOnCollision = true, clearVelocityOnCollision = true, preventInsideCollider = false;

	public void applyMotion(double[] coords, double[] velocity, double inertia, Collider... colliders) {
		for (int i = 0; i < coords.length; i++)
			for (Collider collider : colliders) {
				boolean hasObstacleAtBefore = collider.hasCollisionAt(coords);
				coords[i] += velocity[i] / inertia;
				if ((!hasObstacleAtBefore || preventInsideCollider) && collider.hasCollisionAt(coords)) {
					if (preventMotionOnCollision)
						coords[i] -= velocity[i] / inertia;
					if (clearVelocityOnCollision)
						velocity[i] = 0;
				} else velocity[i] -= velocity[i] / inertia;
			} // Problem: here used extrapolation, with big velocity player launches through
				// Don't give velocity? I make modes
	}

	public double getInertia() { return inertia; }

	public double getKinematics() { return kinematics; }
}
