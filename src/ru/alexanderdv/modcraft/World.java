package ru.alexanderdv.modcraft;

import java.util.Random;

import ru.alexanderdv.modcraft.Block.Side6;
import ru.alexanderdv.utils.MathUtils;

public class World {
	public static interface Generation { Block getBlock(int x, int y, int z, World w); }

	public static enum GenerationType implements Generation {
		RANDOM { public Block getBlock(int x, int y, int z, World w) { return new Block(x, y, z, w, MathUtils.loopI(w.getBlock(x, y, z).id + new Random().nextInt(), 0, Block.names.size())); } },
		AIR_ON_TOP { public Block getBlock(int x, int y, int z, World w) { return new Block(x, y, z, w, y > 16 ? 0 : w.getBlock(x, y, z).id); } };

		public abstract Block getBlock(int x, int y, int z, World w);
	}

	int xSize, ySize, zSize;
	Block[] blocks;
	boolean[][] needHide;
	int chunkSize = 16;

	private int calcArrSize() { return xSize * ySize * zSize; }

	private int toPosInArr(int x, int y, int z) { return x * ySize * zSize + y * zSize + z; }

	public World(int width, int height, int depth, Generation... generations) {
		this.xSize = width;
		this.ySize = height;
		this.zSize = depth;
		blocks = new Block[calcArrSize()];
		needHide = new boolean[calcArrSize()][Side6.values().length];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				for (int z = 0; z < depth; z++) {
					blocks[toPosInArr(x, y, z)] = new Block(x, y, z, this, 0);
					for (Generation generation : generations)
						blocks[toPosInArr(x, y, z)] = generation.getBlock(x, y, z, this);
				}
	}

	boolean loop = true, clamp;

	private int toPosInArrBySettings(int x, int y, int z) { return loop ? MathUtils.loopI(toPosInArr(x, y, z), 0, calcArrSize()) : MathUtils.clampI(toPosInArr(x, y, z), 0, calcArrSize()); }

	public void setBlock(int x, int y, int z, int id) { blocks[toPosInArrBySettings(x, y, z)].id = id; }

	public Block getBlock(int x, int y, int z) { return blocks[toPosInArrBySettings(x, y, z)]; }

	public void calcNeedHide(int x, int y, int z) {
		setNeedHide(x, y, z, getBlock(x, y, z).isMeshed() ? new boolean[6]
				: new boolean[] {

						!getBlock(x, y + 1, z).isTransparent(), !getBlock(x, y - 1, z).isTransparent(),

						!getBlock(x, y, z + 1).isTransparent(), !getBlock(x, y, z - 1).isTransparent(),

						!getBlock(x + 1, y, z).isTransparent(), !getBlock(x - 1, y, z).isTransparent(), });
	}

	public boolean[] isNeedHide(int x, int y, int z) { return needHide[toPosInArrBySettings(x, y, z)]; }

	public void setNeedHide(int x, int y, int z, boolean[] r) { needHide[toPosInArrBySettings(x, y, z)] = r; }
}