package ru.alexanderdv.modcraft;

public class World {
	int xSize, ySize, zSize;
	Block[] blocks;
	boolean[][] needHide;
	int chunkSize = 16;

	public World(int width, int height, int depth) {
		this.xSize = width;
		this.ySize = height;
		this.zSize = depth;
		blocks = new Block[width * height * depth];
		needHide = new boolean[width * height * depth][6];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				for (int z = 0; z < depth; z++)
					blocks[x * height * depth + y * depth + z] = new Block(x, y, z, 0);
	}

	public void setBlock(int x, int y, int z, int id) { blocks[((x * ySize * zSize + y * zSize + z) % (xSize * ySize * zSize) + (xSize * ySize * zSize)) % (xSize * ySize * zSize)].id = id; }

	public Block getBlock(int x, int y, int z) { return blocks[((x * ySize * zSize + y * zSize + z) % (xSize * ySize * zSize) + (xSize * ySize * zSize)) % (xSize * ySize * zSize)]; }

	public boolean[] needHide(int x, int y, int z) {
		needHide[((x * ySize * zSize + y * zSize + z) % (xSize * ySize * zSize) + (xSize * ySize * zSize)) % (xSize * ySize * zSize)] = new boolean[] {

				!getBlock(x, y + 1, z).isTransparent(), !getBlock(x, y - 1, z).isTransparent(),

				!getBlock(x, y, z + 1).isTransparent(), !getBlock(x, y, z - 1).isTransparent(),

				!getBlock(x + 1, y, z).isTransparent(), !getBlock(x - 1, y, z).isTransparent(), };

		return needHide[((x * ySize * zSize + y * zSize + z) % (xSize * ySize * zSize) + (xSize * ySize * zSize)) % (xSize * ySize * zSize)];
	}

	public void setNeedHide(int x, int y, int z, boolean[] r) { needHide[((x * ySize * zSize + y * zSize + z) % (xSize * ySize * zSize) + (xSize * ySize * zSize)) % (xSize * ySize * zSize)] = r; }

	public boolean isTransparent(int x, int y, int z) { return getBlock(x, y, z).isTransparent(); }

	public boolean isLiquid(int x, int y, int z) { return getBlock(x, y, z).isLiquid(); }

	public boolean isGas(int x, int y, int z) { return getBlock(x, y, z).isGas(); }

	public boolean isSolid(int x, int y, int z) { return getBlock(x, y, z).isSolid(); }

	public boolean isVoid(int x, int y, int z) { return getBlock(x, y, z).isVoid(); }
}