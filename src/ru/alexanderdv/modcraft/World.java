package ru.alexanderdv.modcraft;

import java.util.Random;

import ru.alexanderdv.modcraft.Block.Side;
import ru.alexanderdv.modcraft.PhysicalPOV.Collider;
import ru.alexanderdv.modcraft.PhysicalPOV.PhysicalEnviroment;
import ru.alexanderdv.utils.MathUtils;

public class World {
	public static interface Generation { Block getBlock(int x, int y, int z, World w); }

	public static enum GenerationType implements Generation {
		RANDOM { public Block getBlock(int x, int y, int z, World w) { return new Block(x, y, z, w, MathUtils.loopI(w.getBlock(x, y, z).id + new Random().nextInt(), 0, Block.names.size())); } },
		AIR_ON_TOP { public Block getBlock(int x, int y, int z, World w) { return new Block(x, y, z, w, y > 16 ? 0 : w.getBlock(x, y, z).id); } };

		public abstract Block getBlock(int x, int y, int z, World w);
	}

	int[] size;
	Block[] blocks;
	boolean[][] needHide;
	int chunkSize = 16;

	private int calcArrSize() { return size[0] * size[1] * size[2]; }

	private int toPosInArr(int x, int y, int z) { return x * size[1] * size[2] + y * size[2] + z; }

	public World(int width, int height, int depth, Generation... generations) {
		this.size = new int[] { width, height, depth };
		blocks = new Block[calcArrSize()];
		needHide = new boolean[calcArrSize()][Side.values().length];
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				for (int z = 0; z < depth; z++) {
					blocks[toPosInArr(x, y, z)] = new Block(x, y, z, this, 0);
					for (Generation generation : generations)
						blocks[toPosInArr(x, y, z)] = generation.getBlock(x, y, z, this);
				}
	}

	boolean loop = true, clamp = false;

	private int toPosInArrByFlags(int x, int y, int z) { return loop ? MathUtils.loopI(toPosInArr(x, y, z), 0, calcArrSize()) : MathUtils.clampI(toPosInArr(x, y, z), 0, calcArrSize()); }

	public void setBlock(int x, int y, int z, int id) { blocks[toPosInArrByFlags(x, y, z)].id = id; }

	public Block getBlock(int x, int y, int z) { return blocks[toPosInArrByFlags(x, y, z)]; }

	public void calcNeedHide(int x, int y, int z) {
		setNeedHide(x, y, z, getBlock(x, y, z).isMeshed() ? new boolean[6]
				: new boolean[] {

						!getBlock(x + 1, y, z).isTransparent(), !getBlock(x - 1, y, z).isTransparent(),

						!getBlock(x, y + 1, z).isTransparent(), !getBlock(x, y - 1, z).isTransparent(),

						!getBlock(x, y, z + 1).isTransparent(), !getBlock(x, y, z - 1).isTransparent(), });
	}

	public boolean[] isNeedHide(int x, int y, int z) { return needHide[toPosInArrByFlags(x, y, z)]; }

	public void setNeedHide(int x, int y, int z, boolean[] r) { needHide[toPosInArrByFlags(x, y, z)] = r; }

	boolean border = true, blocksCollision = true;

	Collider worldCollider = (double[] position) -> {
		for (int i = 0; i < 3; i++)
			if (position[i] < 0 || position[i] > size[i])
				return false;
		return true;
	}, nonSolidBlocksCollider = (double[] position) -> { return getBlock((int) position[0], (int) (position[1] - 0.05), (int) position[2]).isCollidable(); },
			humanCollider = (double[] pos) -> { return nonSolidBlocksCollider.hasCollisionAt(pos) || getBlock((int) (pos[0]), (int) (pos[1] - 1.8), (int) (pos[2])).isCollidable(); };

	Collider collider = new Collider() {
		@Override
		public boolean hasCollisionAt(double[] position) {
			if (position.length != size.length)
				return false;
			return (border ? !worldCollider.hasCollisionAt(position) : false) || (blocksCollision ? nonSolidBlocksCollider.hasCollisionAt(position) : false);
		}
	};

	PhysicalEnviroment enviroment = new PhysicalEnviroment() {
		double gravity = 9.8;

		@Override
		public double getGravity() { return gravity; }
	};
}