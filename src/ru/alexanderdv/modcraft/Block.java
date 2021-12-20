package ru.alexanderdv.modcraft;

public class Block {
	public static final String[] names = new String[Textures.names.length];
	String name;
	int id, x, y, z;

	public Block(int x, int y, int z, int id) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.id = id;
	}

	public boolean isTransparent() { return isGas() || isLiquid() || isVoid() || names[id].equals("glass") || names[id].equals("sapling") || names[id].equals("leaves"); }

	public boolean isSolid() { return !isVoid() && !isLiquid() && !isGas(); }

	public boolean isLiquid() { return names[id].equals("water") || names[id].equals("lava"); }

	public boolean isGas() { return names[id].equals("air"); }

	public boolean isVoid() { return id < 0; }
}