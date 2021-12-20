package ru.alexanderdv.modcraft;

import ru.alexanderdv.utils.VectorD;

public class POV extends VectorD {
	protected VectorD rotation = new VectorD(2);

	public VectorD getRotation() { return rotation; }

	public void setRotation(VectorD rotation) { this.rotation.coords = rotation.coords; }

	public double getRotationX() { return rotation.coords[0]; }

	public void setRotationX(double x) { this.rotation.coords[0] = x; }

	public double getRotationY() { return rotation.coords[1]; }

	public void setRotationY(double y) { this.rotation.coords[1] = y; }
}