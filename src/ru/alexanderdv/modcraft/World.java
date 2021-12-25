package ru.alexanderdv.modcraft;

import static ru.alexanderdv.utils.MathUtils.*;
import static ru.alexanderdv.utils.MathUtils.loopI;

import java.util.Random;

import ru.alexanderdv.modcraft.Block.Side;
import ru.alexanderdv.modcraft.PhysicalPOV.Collider;
import ru.alexanderdv.modcraft.PhysicalPOV.PhysicalEnviroment;
import ru.alexanderdv.utils.VectorD;

public class World {
	public static interface Generation { Block getBlock(World world, double x, double y, double z, double w); }

	public static enum GenerationType implements Generation {
		RANDOM {
			public Block getBlock(World world, double x, double y, double z, double w) {

				return new Block(world, (int) x, (int) y, (int) z, (int) w, loopI(world.blocks[world.toPosInArrByFlags(x, y, z, w)].id + new Random().nextInt(), 0, Block.names.size()));
			}
		},
		AIR_ON_TOP {
			public Block getBlock(World world, double x, double y, double z, double w) {

				return new Block(world, (int) x, (int) y, (int) z, (int) w, y > 16 ? 0 : world.blocks[world.toPosInArrByFlags(x, y, z, w)].id);
			}
		};

		public abstract Block getBlock(World world, double x, double y, double z, double w);
	}

	VectorD size;
	Block[] blocks;
	boolean[][] needHide;
	Generation[] generations;

	private int calcArrayLength() {
		int arrayLength = 1;
		for (int i = 0; i < size.coords.length; i++)
			arrayLength *= size.coords[i];
		return arrayLength;
	}

	public World(int width, int height, int depth, int length, Generation... generations) {
		this.size = new VectorD(width, height, depth, length);
		this.generations = generations;
		blocks = new Block[calcArrayLength()];
		needHide = new boolean[calcArrayLength()][Side.values().length];
	}

	boolean loop = true, clamp = false, repeat;

	private int toPosInArr(double x, double y, double z, double w) { return ((((int) x * (int) size.coords[1] + (int) y) * (int) size.coords[2] + (int) z) * (int) size.coords[3] + (int) w); }

	private int toPosInArrByFlags(double x, double y, double z, double w) {
		if (loop) // Use double everywhere before and don't change this this methods
			return toPosInArr(loopD(x, 0, size.getX()), loopD(y, 0, size.getY()), loopD(z, 0, size.getZ()), loopD(w, 0, size.getW()));// else you will have shift artifacts
		return toPosInArr(clampD(x, 0, size.getX() - 1), clampD(y, 0, size.getY() - 1), clampD(z, 0, size.getZ() - 1), clampD(w, 0, size.getW() - 1));// with numbers more than world size
	}

	public void setBlock(double x, double y, double z, double w, int id) { blocks[nullSafeGetPosInArray(x, y, z, w)].id = id; }

	public Block getBlock(double x, double y, double z, double w) { return blocks[nullSafeGetPosInArray(x, y, z, w)]; }

	private int nullSafeGetPosInArray(double x, double y, double z, double w) {
		int posInArray = toPosInArrByFlags(x, y, z, w);
		if (blocks[posInArray] == null) {
			blocks[posInArray] = new Block(this, (int) x, (int) y, (int) z, (int) w, 0);
			for (Generation generation : generations)
				blocks[posInArray] = generation.getBlock(this, x, y, z, w);
		}
		return posInArray;
	}

	public boolean[] isNeedHide(double x, double y, double z, double w) { return needHide[toPosInArrByFlags(x, y, z, w)]; }

	public void setNeedHide(double x, double y, double z, double w, boolean[] r) { needHide[toPosInArrByFlags(x, y, z, w)] = r; }

	public void calcNeedHide(double x, double y, double z, double w) {
		setNeedHide(x, y, z, w, getBlock(x, y, z, w).isMeshed() ? new boolean[6]
				: new boolean[] {

						!getBlock(x + 1, y, z, w).isTransparent(), !getBlock(x - 1, y, z, w).isTransparent(),

						!getBlock(x, y + 1, z, w).isTransparent(), !getBlock(x, y - 1, z, w).isTransparent(),

						!getBlock(x, y, z + 1, w).isTransparent(), !getBlock(x, y, z - 1, w).isTransparent(), });
	}

	boolean border = true, blocksCollision = true;

	Collider worldCollider = (double[] position) -> {
		for (int i = 0; i < 3; i++)
			if (position[i] < 0 || position[i] > size.coords[i])
				return false;
		return true;
	}, nonSolidBlocksCollider = (double[] position) -> { return getBlock(position[0], position[1], position[2], position[3]).isCollidable(); };

	Collider collider = new Collider() {
		@Override
		public boolean hasCollisionAt(double[] position) {
			if (position.length != size.coords.length)
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