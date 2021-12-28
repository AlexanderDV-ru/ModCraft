package ru.alexanderdv.modcraft;

import static ru.alexanderdv.utils.MathUtils.clampD;
import static ru.alexanderdv.utils.MathUtils.loopD;
import static ru.alexanderdv.utils.MathUtils.loopI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Random;

import ru.alexanderdv.modcraft.PhysicalPOV.Collider;
import ru.alexanderdv.modcraft.interfaces.Generation;
import ru.alexanderdv.modcraft.interfaces.IWorld;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.VectorD;

public class World implements IWorld {
	private static final long serialVersionUID = 8984301574021898451L;

	public VectorD size;
	private HashMap<Integer, Block> blocks;
	public Generation[] generations;

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.writeObject(size);
		oos.writeObject(blocks);
		oos.writeObject(generationsToString(generations));
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		size = (VectorD) ois.readObject();
		blocks = (HashMap<Integer, Block>) ois.readObject();
		String generationString = (String) ois.readObject();
		generations = generationsFromString(generationString);
		init();
	}

	public World(int width, int height, int depth, int length, Generation... generations) {
		this.size = new VectorD(width, height, depth, length);
		this.generations = generations;
		blocks = new HashMap<>();
		init();
	}

	public World(int width, int height, int depth, int length, String generationString) { this(width, height, depth, length, generationsFromString(generationString)); }

	public static Generation[] generationsFromString(String generationString) {
		String[] generationLines = generationString.split(",");
		Generation[] generations = new Generation[generationLines.length];
		for (int i = 0; i < generations.length; i++) {
			int o = i;
			String[] pair = generationLines[o].replace("air_on_top", "12-*:0").split(":");
			String idStr = pair[pair.length > 1 ? 1 : 0];
			String[] filter = pair[0].split(",");
			String[][] filterCoords = new String[filter.length][];
			for (int n = 0; n < filter.length && pair.length > 1; n++)
				filterCoords[n] = filter[n].split("-");
			generations[i] = new Generation() {
				@Override
				public Block getBlock(IWorld world, double x, double y, double z, double w) {
					double[] coords = { x, y, z, w };
					for (int n = 0; n < filter.length && pair.length > 1; n++) {
						double coord = coords[filter.length < 2 ? 1 : n];
						if (filterCoords[n].length < 2) {
							if (!(coord < MathUtils.parseI(filterCoords[n][0]) || filterCoords[n][0].equals("*")))
								return null;
						} else if (!(coord >= MathUtils.parseI(filterCoords[n][0]) || filterCoords[n][0].equals("*")))
							return null;
						else if (!(coord <= MathUtils.parseI(filterCoords[n][1]) || filterCoords[n][1].equals("*")))
							return null;
					}
					int id = MathUtils.parseI(idStr);
					if (idStr.equalsIgnoreCase("random"))
						id = loopI(id + new Random().nextInt(), 0, Block.names.size());
					for (int i : Block.names.keySet())
						if (Block.names.get(i).equalsIgnoreCase(idStr))
							id = i;
					return new Block((int) x, (int) y, (int) z, (int) w, id);
				}

				@Override
				public String generationToString() { return generationLines[o]; }
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
		needHide = new HashMap<>();
		gravity = 9.8;
	}

	public void setVoidId(int id) { blocks.put(-1, new Block(-1, -1, -1, -1, id)); }

	HashMap<Integer, boolean[]> needHide;
	public boolean loop = true, clamp = false, dontUseNonInWorldBlock, repeat;

	private int toPosInArr(double x, double y, double z, double w) { return ((((int) x * (int) size.coords[1] + (int) y) * (int) size.coords[2] + (int) z) * (int) size.coords[3] + (int) w); }

	int toPosInArrByFlags(double x, double y, double z, double w) {
		if (loop) // Use double everywhere before and don't change this this methods
			return toPosInArr(loopD(x, 0, size.getX()), loopD(y, 0, size.getY()), loopD(z, 0, size.getZ()), loopD(w, 0, size.getW()));// else you will have shift artifacts
		if (!dontUseNonInWorldBlock)
			return x >= 0 && x < size.getX() && y >= 0 && y < size.getY() && z >= 0 && z < size.getZ() && w >= 0 && w < size.getW() ? toPosInArr(x, y, z, w) : -1;
		return toPosInArr(clampD(x, 0, size.getX() - 1), clampD(y, 0, size.getY() - 1), clampD(z, 0, size.getZ() - 1), clampD(w, 0, size.getW() - 1));// with numbers more than world size
	}

	public void setBlocks(double x1, double y1, double z1, double w1, double x2, double y2, double z2, double w2, int id) {

		setBlocks(x1, y1, z1, w1, x2, y2, z2, w2, id, false, false);
	}

	public void setBlocks(double x1, double y1, double z1, double w1, double x2, double y2, double z2, double w2, int id, boolean dontUpdatePhysics, boolean dontUpdateSides) {
		for (double x = Math.min(x1, x2); x < Math.max(x1, x2) + 1; x++)
			for (double y = Math.min(y1, y2); y < Math.max(y1, y2) + 1; y++)
				for (double z = Math.min(z1, z2); z < Math.max(z1, z2) + 1; z++)
					for (double w = Math.min(w1, w2); w < Math.max(w1, w2) + 1; w++)
						blocks.put(toPosInArrByFlags(x, y, z, w), new Block((int) x, (int) y, (int) z, (int) w, id));
		if (!dontUpdatePhysics)
			for (double x = Math.min(x1, x2); x < Math.max(x1, x2) + 1; x++)
				for (double y = Math.min(y1, y2) - 1; y < Math.max(y1, y2) + 1 + 1; y++)
					for (double z = Math.min(z1, z2); z < Math.max(z1, z2) + 1; z++)
						for (double w = Math.min(w1, w2); w < Math.max(w1, w2) + 1; w++)
							updatePhysics(x, y, z, w);
		if (!dontUpdateSides)
			calcNeedHide(x1, y1, z1, w1, x2, y2, z2, w2);
	}

	public void calcNeedHide(double x1, double y1, double z1, double w1, double x2, double y2, double z2, double w2) {
		for (double x = Math.min(x1, x2) - 1; x < Math.max(x1, x2) + 1 + 1; x++)
			for (double y = Math.min(y1, y2) - 1; y < Math.max(y1, y2) + 1 + 1; y++)
				for (double z = Math.min(z1, z2) - 1; z < Math.max(z1, z2) + 1 + 1; z++)
					for (double w = Math.min(w1, w2) - 1; w < Math.max(w1, w2) + 1 + 1; w++)
						calcNeedHide(x, y, z, w);
	}

	public void updatePhysics(double x, double y, double z, double w) {
		if (blocks.get(toPosInArrByFlags(x, y, z, w)) != null && blocks.get(toPosInArrByFlags(x, y - 1, z, w)) != null)
			if (!blocks.get(toPosInArrByFlags(x, y - 1, z, w)).isSolid()) {
				if (blocks.get(toPosInArrByFlags(x, y, z, w)).getProps().contains(",needground,")) {
					setBlock(x, y, z, w, 0);
				}
				if (getProp(x, y, z, w, "gravity") > getProp(x, y - 1, z, w, "gravity")) {
					int id = blocks.get(toPosInArrByFlags(x, y, z, w)).getId();
					setBlocks(x, y, z, w, x, y, z, w, blocks.get(toPosInArrByFlags(x, y - 1, z, w)).getId(), true, false);
					setBlocks(x, y - 1, z, w, x, y - 1, z, w, id, true, false);
					updatePhysics(x, y + 1, z, w);
					updatePhysics(x, y - 1, z, w);
				}
			}
	}

	public double getProp(double x, double y, double z, double w, String name) {
		if (blocks.get(toPosInArrByFlags(x, y, z, w)).getProps().split("," + name).length > 1)
			return MathUtils.parseD(blocks.get(toPosInArrByFlags(x, y, z, w)).getProps().split("," + name)[1].split(",")[0]);
		return 0;
	}

	public Block getBlock(double x, double y, double z, double w) {
		int posInArray = toPosInArrByFlags(x, y, z, w);
		if (blocks.get(posInArray) == null) {
			setBlocks(x, y, z, w, x, y, z, w, 0, true, true);
			for (Generation generation : generations) {
				Block block = generation.getBlock(this, x, y, z, w);
				if (block != null)
					setBlocks(x, y, z, w, x, y, z, w, block.getId(), true, true);
			}
		}
		return blocks.get(posInArray);
	}

	public Block getBlock(VectorD position) { return getBlock(position.getX(), position.getY(), position.getZ(), position.getW()); }

	public boolean[] isNeedHide(double x, double y, double z, double w) {
		if (needHide.get(toPosInArrByFlags(x, y, z, w)) == null) {
			calcNeedHide(x, y, z, w);
			updatePhysics(x, y, z, w);
		}
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

	public boolean border = true, blocksHaveCollision = true;

	/** Setting in WorldConfig */
	public Collider borderCollider, nonSolidBlocksCollider, collider;

	public double gravity;

	@Override
	public double getGravity() { return gravity; }

	@Override
	public Collider getCollider() { return collider; }

	@Override
	public boolean isRepeat() { return repeat; }

	@Override
	public VectorD getSize() { return size; }
}