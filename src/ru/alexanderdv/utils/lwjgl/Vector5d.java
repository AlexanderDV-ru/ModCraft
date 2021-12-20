package ru.alexanderdv.utils.lwjgl;

import org.lwjgl.util.vector.Vector4f;

public class Vector5d extends Vector4f implements Cloneable {
	public static interface IWorld { Vector5d getLocation(); }

	private static final long serialVersionUID = 1770577399275758667L;
	public double t;// TODO how to work with time? Marks are other comments;

	public Vector5d(float x, float y, float z, float w, double t) {
		super(x, y, z, w);
		this.t = t;
	}

	public Vector5d(float x, float y, float z, float w) { this(x, y, z, w, System.currentTimeMillis()); }

	public Vector5d(float x, float y, float z, IWorld w) { this(x, y, z, w.getLocation().w); }

	@Override
	public Vector5d clone() { return new Vector5d(x, y, z, w, t); }

	@Override
	public String toString() { return "Vector5d{x=" + x + ", y=" + y + ", z=" + z + ", w=" + w + ", t=" + t + "}"; }

	@Override
	public Vector5d scale(float s) { return new Vector5d(x * s, y * s, z * s, w * s, t/* TODO need *s? */); }

	public void transfer(Vector5d from, Vector5d to) {
		from.x -= x;
		from.y -= y;
		from.z -= z;
		from.w -= w;
		// from.t-=t?;
		to.x += x;
		to.y += y;
		to.z += z;
		to.w += w;
		to.w += w;
		// to.t+=t?;
	}
}
