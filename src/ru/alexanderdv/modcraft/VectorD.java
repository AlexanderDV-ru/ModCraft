package ru.alexanderdv.modcraft;

import ru.alexanderdv.utils.ParserDV;

public class VectorD {
	protected double[] coords;

	public VectorD(double... coords) {
		super();
		this.coords = coords;
	}

	public VectorD(int size) { this(new double[size]); }

	public VectorD() { this(3); }

	public int size() { return coords.length; }

	public double length() { return ParserDV.distance(0, 0, 0, coords[0], coords[1], coords[2]); }

	public double[] getCoords() { return coords; }

	public void setCoords(double[] coords) { for (int coordNum = 0; coordNum < Math.min(this.coords.length, coords.length); coordNum++) { this.coords[coordNum] = coords[coordNum]; } }

	public double getX() { return coords[0]; }

	public void setX(double x) { coords[0] = x; }

	public double getY() { return coords[1]; }

	public void setY(double y) { coords[1] = y; }

	public double getZ() { return coords[2]; }

	public void setZ(double z) { coords[2] = z; }

	public double getW() { return coords[3]; }

	public void setW(double w) { coords[3] = w; }

	public double getT() { return coords[4]; }

	public void setT(double t) { coords[4] = t; }
}