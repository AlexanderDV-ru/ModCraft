package ru.alexanderdv.modcraft;

import java.util.HashMap;

import ru.alexanderdv.modcraft.Config.SConfig;
import ru.alexanderdv.modcraft.World.Generation;
import ru.alexanderdv.modcraft.World.GenerationType;
import ru.alexanderdv.utils.MathUtils;

public class WorldManager {

	static String[] sp = { "\n", ";", "," };

	public World loadWorld(String path) {
		String saveTxt = new SConfig(path).readConfigText(Config.defaults.savesPath + path);
		String[][][] saveIds = new String[saveTxt.split(sp[0]).length][saveTxt.split(sp[0])[0].split(sp[1]).length][saveTxt.split(sp[0])[0].split(sp[1])[0].split(sp[2]).length];
		for (int x = 0; x < saveTxt.split(sp[0]).length; x++)
			if (saveTxt.split(sp[0])[x] != null && !saveTxt.split(sp[0])[x].equals(""))
				for (int y = 0; y < saveTxt.split(sp[0])[x].split(sp[1]).length; y++)
					if (saveTxt.split(sp[0])[x].split(sp[1])[y] != null && !saveTxt.split(sp[0])[x].split(sp[1])[y].equals(""))
						try {
							saveIds[x][y] = saveTxt.split(sp[0])[x].split(sp[1])[y].split(sp[2]);
						} catch (Exception e) {
							e.printStackTrace();
						}
		if (saveIds.length < 2 || saveIds[0].length < 2 || saveIds[0][0].length < 2)
			throw new RuntimeException("Zero world size");
		World loadedWorld = new World(saveIds.length, saveIds[0].length, saveIds[0][0].length);
		for (int x = 0; x < saveIds.length; x++)
			for (int y = 0; y < saveIds[x].length; y++)
				for (int z = 0; z < saveIds[x][y].length; z++)
					try {
						loadedWorld.setBlock(x, y, z, MathUtils.parseI(saveIds[x][y][z]));
					} catch (Exception e) {}
		return loadedWorld;
	}

	public World generateWorld() {
		Config<String> worldCustom = new Config<String>(Config.defaults.args);
		String[] worldCfgLines = worldCustom.readConfigLines(worldCustom.configsS + "/world" + worldCustom.cfgExtS);
		Generation[] generations = new Generation[worldCfgLines.length - 5];
		for (int i = 0; i < generations.length; i++)
			try {
				generations[i] = GenerationType.valueOf(worldCfgLines[i + 5].toUpperCase());
			} catch (Exception e) {
				final int o = i;
				generations[i] = new Generation() {
					@Override
					public Block getBlock(int x, int y, int z, World w) {
						if (y < MathUtils.parseI(worldCfgLines[o + 5].split(":")[0]))
							return new Block(x, y, z, w, MathUtils.parseI(worldCfgLines[o + 5].split(":")[1]));
						else return w.getBlock(x, y, z);
					}

				};
			}
		World generatedWorld = new World(MathUtils.parseI(worldCfgLines[0]), MathUtils.parseI(worldCfgLines[1]), MathUtils.parseI(worldCfgLines[2]), generations);
		generatedWorld.border = worldCfgLines[3].toLowerCase().contains("true");
		generatedWorld.blocksCollision = worldCfgLines[4].toLowerCase().contains("true");
		return generatedWorld;
	}

	HashMap<String, World> worlds = new HashMap<>();

	public World getWorld(String path) {
		World world = worlds.get(path);
		if (world == null)
			try {
				world = loadWorld(path);
			} catch (Exception e) {
				world = generateWorld();
			}
		worlds.put(path, world);
		return world;
	}

	public void saveWorld(World world) {
		for (String path : worlds.keySet())
			if (world == worlds.get(path)) {
				saveWorld(world, path);
				return;
			}
		saveWorld(world, "saves/" + System.nanoTime() + ".txt");
		throw new RuntimeException("No world path, saved by time");
	}

	public void saveWorld(World world, String path) {
		Config<String> cfg = null;
		try {
			cfg = new Config<String>("".split(""));
		} catch (Exception e) {}
		String text = "";
		for (int x = 0; x < world.size[0]; x++, text += sp[0])
			for (int y = 0; y < world.size[1]; y++, text += sp[1])
				for (int z = 0; z < world.size[2]; z++, text += sp[2])
					text += world.getBlock(x, y, z).id;
		cfg.saveConfigText(path, text);
	}
}
