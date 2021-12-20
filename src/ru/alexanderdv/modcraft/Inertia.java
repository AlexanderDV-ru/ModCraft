package ru.alexanderdv.modcraft;

import ru.alexanderdv.utils.Calculable;

public class Inertia implements Calculable {
	private double coord, delta;
	private double speed, inertia;

	public void setCoordDelta(double coord, double delta) {
		this.coord = coord;
		this.delta = delta;
	}

	public void setSpeedInertia(double speed, double inertia) {
		this.speed = speed;
		this.inertia = inertia;
	}

	public void calculate() {
		double slowing = 1 / speed;
		if (delta / inertia > inertia) {
			coord += inertia / slowing;
			delta -= inertia * slowing;
		} else if (delta / inertia < -inertia) {
			coord -= inertia / slowing;
			delta += inertia * slowing;
		} else {
			coord += delta / inertia / slowing;
			delta -= delta / inertia * slowing;
		}
	}

	public double getCoord() { return coord; }

	public double getDelta() { return delta; }
}