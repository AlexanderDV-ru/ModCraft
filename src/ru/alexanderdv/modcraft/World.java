package ru.alexanderdv.modcraft;

import java.util.Random;

import ru.alexanderdv.utils.ParserDV;

public class World {
	public static interface Generation { Block getBlock(int x, int y, int z, World w); }

	public static enum GenerationType implements Generation {
		RANDOM { public Block getBlock(int x, int y, int z, World w) { return new Block(x, y, z, w, ParserDV.bound(new Random().nextInt(), 0, Block.names.size())); } };

		public abstract Block getBlock(int x, int y, int z, World w);
	}

	int xSize, ySize, zSize;
	Block[] blocks;
	boolean[][] needHide;
	int chunkSize = 16;

	public World(int width, int height, int depth, Generation generation) {
		this.xSize = width;
		this.ySize = height;
		this.zSize = depth;
		blocks = new Block[width * height * depth];
		needHide = new boolean[width * height * depth][6];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				for (int z = 0; z < depth; z++)
					blocks[x * height * depth + y * depth + z] = generation.getBlock(x, y, z, this);
	}

	public void setBlock(int x, int y, int z, int id) { blocks[((x * ySize * zSize + y * zSize + z) % (xSize * ySize * zSize) + (xSize * ySize * zSize)) % (xSize * ySize * zSize)].id = id; }

	public Block getBlock(int x, int y, int z) { return blocks[((x * ySize * zSize + y * zSize + z) % (xSize * ySize * zSize) + (xSize * ySize * zSize)) % (xSize * ySize * zSize)]; }

	public void calcNeedHide(int x, int y, int z) {
		setNeedHide(x, y, z, new boolean[] {

				!getBlock(x, y + 1, z).isTransparent(), !getBlock(x, y - 1, z).isTransparent(),

				!getBlock(x, y, z + 1).isTransparent(), !getBlock(x, y, z - 1).isTransparent(),

				!getBlock(x + 1, y, z).isTransparent(), !getBlock(x - 1, y, z).isTransparent(), });
	}

	public boolean[] isNeedHide(int x, int y, int z) { return needHide[((x * ySize * zSize + y * zSize + z) % (xSize * ySize * zSize) + (xSize * ySize * zSize)) % (xSize * ySize * zSize)]; }

	public void setNeedHide(int x, int y, int z, boolean[] r) { needHide[((x * ySize * zSize + y * zSize + z) % (xSize * ySize * zSize) + (xSize * ySize * zSize)) % (xSize * ySize * zSize)] = r; }

	public boolean isTransparent(int x, int y, int z) { return getBlock(x, y, z).isTransparent(); }

	public boolean isLiquid(int x, int y, int z) { return getBlock(x, y, z).isLiquid(); }

	public boolean isGas(int x, int y, int z) { return getBlock(x, y, z).isGas(); }

	public boolean isSolid(int x, int y, int z) { return getBlock(x, y, z).isSolid(); }

	public boolean isVoid(int x, int y, int z) { return getBlock(x, y, z).isVoid(); }
}