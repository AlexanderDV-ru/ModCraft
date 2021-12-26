package ru.alexanderdv.modcraft;

import static ru.alexanderdv.utils.MathUtils.clampD;
import static ru.alexanderdv.utils.MathUtils.loopD;
import static ru.alexanderdv.utils.MathUtils.loopI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Random;

import ru.alexanderdv.modcraft.PhysicalPOV.Collider;
import ru.alexanderdv.modcraft.PhysicalPOV.PhysicalEnviroment;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.VectorD;

public class World implements Serializable, PhysicalEnviroment {
	private static final long serialVersionUID = 8984301574021898451L;

	VectorD size;
	HashMap<Integer, Block> blocks;
	Generation[] generations;

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.writeObject(size);
		oos.writeObject(blocks);
		oos.writeObject(generationsToString(generations));
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		init();
		size = (VectorD) ois.readObject();
		blocks = (HashMap<Integer, Block>) ois.readObject();
		String generationString = (String) ois.readObject();
		generations = generationsFromString(generationString);
	}

	public World(int width, int height, int depth, int length, Generation... generations) {
		this.size = new VectorD(width, height, depth, length);
		this.generations = generations;
		init();
	}

	public World(int width, int height, int depth, int length, String generationString) { this(width, height, depth, length, generationsFromString(generationString)); }

	public static Generation[] generationsFromString(String generationString) {
		String[] generationStrings = generationString.split(",");
		Generation[] generations = new Generation[generationStrings.length];
		for (int i = 0; i < generations.length; i++) {
			final int o = i;
			generations[i] = new Generation() {
				@Override
				public Block getBlock(World world, double x, double y, double z, double w) {
					generationStrings[o] = generationStrings[o].replace("air_on_top", "12-*:0");
					double[] coords = { x, y, z, w };
					if (generationStrings[o].split(":").length > 1) {
						String[] formula = generationStrings[o].split(":")[0].split(",");
						for (int i = 0; i < formula.length; i++)
							if (formula[i].split("-").length < 2) {
								if (!(coords[formula.length < 2 ? 1 : i] < MathUtils.parseI(formula[i].split("-")[0]) || formula[i].split("-")[0].equals("*")))
									return world.getBlock(x, y, z, w);
							} else if (!(coords[formula.length < 2 ? 1 : i] >= MathUtils.parseI(formula[i].split("-")[0]) || formula[i].split("-")[0].equals("*")))
								return world.getBlock(x, y, z, w);
							else if (!(coords[formula.length < 2 ? 1 : i] <= MathUtils.parseI(formula[i].split("-")[1]) || formula[i].split("-")[1].equals("*")))
								return world.getBlock(x, y, z, w);
					}
					int c = generationStrings[o].split(":").length < 2 ? 0 : 1;
					int id = MathUtils.parseI(generationStrings[o].split(":")[c]);
					if (generationStrings[o].split(":")[c].equalsIgnoreCase("random"))
						id = loopI(world.blocks.get(world.toPosInArrByFlags(x, y, z, w)).id + new Random().nextInt(), 0, Block.names.size());
					for (int i : Block.names.keySet())
						if (Block.names.get(i).equalsIgnoreCase(generationStrings[o].split(":")[c]))
							id = i;
					return new Block((int) x, (int) y, (int) z, (int) w, id);
				}

				@Override
				public String generationToString() { return generationStrings[o]; }
			};
		}
		return generations;
	}

	private String generationsToString(Generation... generations) {
		String generationString = "";
		for (Generation generation : generations)
			generationString += generation.generationToString() + ",";
		return generationString;
	}

	private void init() {
		blocks = new HashMap<>();
		needHide = new HashMap<>();
	}

	HashMap<Integer, boolean[]> needHide;
	boolean loop = true, clamp = false, repeat;

	private int toPosInArr(double x, double y, double z, double w) { return ((((int) x * (int) size.coords[1] + (int) y) * (int) size.coords[2] + (int) z) * (int) size.coords[3] + (int) w); }

	int toPosInArrByFlags(double x, double y, double z, double w) {
		if (loop) // Use double everywhere before and don't change this this methods
			return toPosInArr(loopD(x, 0, size.getX()), loopD(y, 0, size.getY()), loopD(z, 0, size.getZ()), loopD(w, 0, size.getW()));// else you will have shift artifacts
		return toPosInArr(clampD(x, 0, size.getX() - 1), clampD(y, 0, size.getY() - 1), clampD(z, 0, size.getZ() - 1), clampD(w, 0, size.getW() - 1));// with numbers more than world size
	}

	public void setBlock(double x, double y, double z, double w, int id) { blocks.get(nullSafeGetPosInArray(x, y, z, w)).id = id; }

	public Block getBlock(double x, double y, double z, double w) { return blocks.get(nullSafeGetPosInArray(x, y, z, w)); }

	private int nullSafeGetPosInArray(double x, double y, double z, double w) {
		int posInArray = toPosInArrByFlags(x, y, z, w);
		if (blocks.get(posInArray) == null) {
			blocks.put(posInArray, new Block((int) x, (int) y, (int) z, (int) w, 0));
			for (Generation generation : generations)
				blocks.put(posInArray, generation.getBlock(this, x, y, z, w));
		}
		return posInArray;
	}

	public boolean[] isNeedHide(double x, double y, double z, double w) {
		if (needHide.get(toPosInArrByFlags(x, y, z, w)) == null)
			calcNeedHide(x, y, z, w);
		return needHide.get(toPosInArrByFlags(x, y, z, w));
	}

	public void setNeedHide(double x, double y, double z, double w, boolean[] r) { needHide.put(toPosInArrByFlags(x, y, z, w), r); }

	public void calcNeedHide(double x, double y, double z, double w) {
		setNeedHide(x, y, z, w, getBlock(x, y, z, w).isMeshed() ? new boolean[6]
				: new boolean[] {

						!getBlock(x + 1, y, z, w).isTransparent(), !getBlock(x - 1, y, z, w).isTransparent(),

						!getBlock(x, y + 1, z, w).isTransparent(), !getBlock(x, y - 1, z, w).isTransparent(),

						!getBlock(x, y, z + 1, w).isTransparent(), !getBlock(x, y, z - 1, w).isTransparent(), });
	}

	boolean border = true, blocksHaveCollision = true;

	/** Setting in WorldConfig */
	Collider borderCollider, nonSolidBlocksCollider, collider;

	double gravity = 9.8;

	@Override
	public double getGravity() { return gravity; }
}