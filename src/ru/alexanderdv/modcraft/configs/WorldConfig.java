package ru.alexanderdv.modcraft.configs;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ru.alexanderdv.modcraft.World;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.MessageSystem.Msgs;

public class WorldConfig extends SConfig {
	private static final long serialVersionUID = -935320013479956125L;

	public WorldConfig(String path) { super(path); }

	static String[] sp = { "\n", ";", ",", "'" };

	public void loadWorldTxt(String path) {
		String saveTxt = String.join(sp[0], lines);
		String[] xSplit = saveTxt.split(sp[0]), ySplit = xSplit[0].split(sp[1]), zSplit = ySplit[0].split(sp[2]), wSplit = zSplit[0].split(sp[3]);
		world = new World(xSplit.length, ySplit.length, zSplit.length, wSplit.length, "12-*:0");
		new Thread(() -> {
			String[][][][] saveIds = new String[xSplit.length][ySplit.length][zSplit.length][wSplit.length];
			for (int x = 0; x < saveTxt.split(sp[0]).length; x++)
				if (stringIsNotEmpty(saveTxt.split(sp[0])[x]))
					for (int y = 0; y < saveTxt.split(sp[0])[x].split(sp[1]).length; y++)
						if (stringIsNotEmpty(saveTxt.split(sp[0])[x].split(sp[1])[y]))
							for (int z = 0; z < saveTxt.split(sp[0])[x].split(sp[1])[y].split(sp[2]).length; z++)
								if (stringIsNotEmpty(saveTxt.split(sp[0])[x].split(sp[1])[y].split(sp[2])[z]))
									try {
										saveIds[x][y][z] = saveTxt.split(sp[0])[x].split(sp[1])[y].split(sp[2])[z].split(sp[3]);
									} catch (Exception e) {
										e.printStackTrace();
									}
			for (int i = 0; i < world.size.size(); i++)
				if (world.size.coords[i] < 2)
					throw new RuntimeException("Zero world size");
			for (int x = 0; x < saveIds.length; x++)
				for (int y = 0; y < saveIds[x].length; y++)
					for (int z = 0; z < saveIds[x][y].length; z++)
						for (int w = 0; w < saveIds[x][y][z].length; w++)
							try {
								world.setBlock(x, y, z, w, MathUtils.parseI(saveIds[x][y][z][w]));
							} catch (Exception e) {
								e.printStackTrace();
							}
			Thread.currentThread().interrupt();
		}).start();
	}

	private SConfig getCfg() {
		SConfig savesCfg = new SConfig(_path.replace(".save", ".cfg"));
		SConfig worldCfg = new SConfig("configs/world.cfg");
		return savesCfg.lines != null && savesCfg.lines.length != 0 ? savesCfg : worldCfg;
	}

	private void setWorldByConfig() {
		SConfig cfg = getCfg();
		world.blocksHaveCollision = cfg.bool("blocksCollision");
		world.border = cfg.bool("border");
		world.clamp = cfg.bool("clamp");
		world.repeat = cfg.bool("repeat");
		world.loop = cfg.bool("loop");
	}

	private void setWorldLambdas() {
		world.nonSolidBlocksCollider = (double[] position) -> { return world.getBlock(position[0], position[1], position[2], position[3]).isCollidable(); };
		world.borderCollider = (double[] position) -> {
			for (int i = 0; i < 3; i++)
				if (position[i] < 0 || position[i] > world.size.coords[i])
					return false;
			return true;
		};
		world.collider = (double[] position) -> {
			if (position.length != world.size.coords.length)
				return false;
			return (world.border ? !world.borderCollider.hasCollisionAt(position) : false) || (world.blocksHaveCollision ? world.nonSolidBlocksCollider.hasCollisionAt(position) : false);
		};
	}

	World world;

	public void loadWorldDat() throws Exception {
		ObjectInputStream objectInputStream = new ObjectInputStream(getConfigInputStream(getSavingPath()));
		world = (World) objectInputStream.readObject();
		objectInputStream.close();
	}

	public void saveWorldTxt(World world) throws Exception {
		SConfig cfg = new SConfig(getSavingPath());
		String text = "";
		for (int x = 0; x < world.size.coords[0]; x++, text += sp[0])
			for (int y = 0; y < world.size.coords[1]; y++, text += sp[1])
				for (int z = 0; z < world.size.coords[2]; z++, text += sp[2])
					for (int w = 0; w < world.size.coords[3]; w++, text += sp[3])
						text += world.getBlock(x, y, z, w).id;
		cfg.saveConfigText(_path.replace(".save", ".txt"), text);
	}

	public void saveWorldDat(World world) throws Exception {
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(getOutputStream(getSavingPath()));
		objectOutputStream.writeObject(world);
		objectOutputStream.flush();
		objectOutputStream.close();
	}

	public void saveWorld(World world) {
		SConfig cfgCfg = getCfg();
		Msgs.last.debug("Saving world to '" + getSavingPath() + "'...");
		try {
			if (cfgCfg.get("type").equalsIgnoreCase("txt"))
				saveWorldTxt(world);
			else saveWorldDat(world);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public World getWorld() {
		SConfig cfg = getCfg();
		if (world == null)
			try {
				try {
					Msgs.last.debug("Loading world from '" + getSavingPath().replace(".txt", ".dat").replace(".save", ".dat") + "'...");
					loadWorldDat();
				} catch (Exception e) {
					Msgs.last.debug("Loading world from '" + getSavingPath().replace(".dat", ".txt").replace(".save", ".txt") + "'...");
					loadWorldTxt(_path);
				}
			} catch (Exception e) {
				Msgs.last.debug("Can not load the world! Generating world...");
				world = new World((int) cfg.num("xSize", 0), (int) cfg.num("ySize", 0), (int) cfg.num("zSize", 0), (int) cfg.num("wSize", 0), String.join(",", cfg.lines));
			} finally {
				setWorldByConfig();
				setWorldLambdas();
			}
		return world;
	}

	public String getSavingPath() { return configuredPath(getCfg().get("type").equalsIgnoreCase("txt") ? _path.replace(".save", ".txt") : _path.replace(".save", ".dat")); }
}