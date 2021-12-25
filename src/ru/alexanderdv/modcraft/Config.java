package ru.alexanderdv.modcraft;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.imageio.ImageIO;

import ru.alexanderdv.modcraft.Controller.UserController;
import ru.alexanderdv.modcraft.World.Generation;
import ru.alexanderdv.modcraft.World.GenerationType;
import ru.alexanderdv.utils.ExceptionsHandler;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.MessageSystem.Msgs;
import ru.alexanderdv.utils.VectorD;

public class Config<Type> extends HashMap<String, Type> {
	private static final long serialVersionUID = -1536270919370232799L;

	public static Config<String> defaults, paths;

	public String rawReplaceR = "([ \"])|([/][*][^/]*[*][/])|(([/][/]|[#])[^\n]*)", newElementS = "\n", orS = "||", typeS = "_", setS = ":";
	public String assetsPath, configPath = "config/", savesPath = "saves/";
	public final String[] args;

	public Config(String[] args) {
		this.args = args;
		assetsPath = "assets/" + args[0] + "/";
		if (defaults != null)
			try {
				String[] lines = readConfigText("configs/configs.cfg").split(newElementS);
				rawReplaceR = lines[0];
				newElementS = unshieldSpecial(lines[1]);
				orS = lines[2];
				typeS = lines[3];
				setS = lines[4];
			} catch (Exception e) {}
	}

	public String shieldSpecial(String string) { return "[" + String.join("][", orS.split("")) + "]"; }

	public String unshieldSpecial(String specialString) { return specialString.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t"); }

	public String[] orSplit(String control) { return control.split(shieldSpecial(orS)); }

	public boolean stringIsNotEmpty(String s) { return s != null && !s.equals(""); }

	// Args

	static String args(String... args) { return String.join(" ", args); }

	public static boolean hasArg(String name, String... args) { return args(args).contains(name); }

	public static String getArg(String name, String... args) { return args(args).split(name)[1].split(" ")[1]; }

	public static String getArg(int number, String... args) { return args(args).split(" ")[number]; }

	public static double getNum(String name, String... args) { return Double.parseDouble(getArg(name, args)); }

	public static int getInt(String name, String... args) { return (int) getNum(name, args); }

	// Read

	static Charset defaultCharset = Charset.forName("UTF-8");

	public InputStream getInputStream(String path) {
		try {
			return Files.newInputStream(Paths.get(new File(path).getAbsolutePath()));
		} catch (Exception e) {
			try {
				InputStream stream = Config.class.getResourceAsStream("/" + path);
				stream.available();
				return stream;
			} catch (Exception e2) {
				throw new RuntimeException("Error with reading from " + path, e2);
			}
		}
	}

	public InputStream getConfigInputStream(String path) {
		path = configuredPath(path);
		try {
			return getInputStream(savesPath + path);
		} catch (Exception e4) {
			try {
				return getInputStream(configPath + path);
			} catch (Exception e) {
				try {
					return getInputStream(assetsPath + path);
				} catch (Exception e1) {
					try {
						return getInputStream(assetsPath + "configs/" + path.split("/")[path.split("/").length - 1]);
					} catch (Exception e2) {
						try {
							return getInputStream(path);
						} catch (Exception e5) {
							throw new RuntimeException("Error with reading from " + path, e.getCause());
						}
					}
				}
			}
		}
	}

	public BufferedImage readConfigImage(String path) {
		try {
			return ImageIO.read(getConfigInputStream(path));
		} catch (Exception e2) {
			throw new RuntimeException("Error with reading from " + path, e2);
		}
	}

	public String readConfigText(String path) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(getConfigInputStream(path), defaultCharset));
			String lines = "";
			for (String line; (line = reader.readLine()) != null;)
				lines += line + "\n";
			reader.close();
			return lines;
		} catch (Exception e2) {
			throw new RuntimeException("Error with reading from " + path, e2);
		}
	}

	public void saveConfigText(String path, String text) {
		path = configuredPath(path);
		try {
			File f = new File(savesPath + path);
			ExceptionsHandler.tryCatchVoid(() -> f.getParentFile().mkdirs());
			ExceptionsHandler.tryCatchVoid(() -> f.createNewFile());
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get(f.getAbsolutePath())), defaultCharset));
			writer.write(text);
			writer.close();
		} catch (Exception e2) {
			throw new RuntimeException("Error with writing to " + savesPath + path, e2);
		}
	}

	public String configuredPath(String path) {
		if (!path.contains("paths.cfg"))
			for (String defaultPath : paths.keySet())
				path = path.replace(defaultPath, paths.get(defaultPath));
		return path;
	}

	public String[] readConfigLines(String path) { return readConfigText(path).replaceAll(rawReplaceR, "").replace("\\s", " ").split(newElementS); }

	public static class SConfig extends Config<String> {
		private static final long serialVersionUID = -165168986796274757L;

		@Override
		public String get(Object key) { return super.get(key) != null ? super.get(key) : "undefined"; }

		public String get(Object key, String defaultValue) { return bool(key) ? get(key) : defaultValue; }

		public boolean bool(Object key) { return !get(key).matches("false|undefined|default|null|[0.]+|not|no|n|[ ]*"); }

		public double num(Object key, double defaultValue) { return MathUtils.parseD(get(key, defaultValue + "")); }

		public String axis(String key, String defaultValue) {

			return get(key, get(key.replace("0", "x").replace("1", "y").replace("2", "z").replace("3", "w").replace("4", "t"), get(key.replaceAll("[0-9]|[.][0-9]", ""), defaultValue)));
		}

		public double coord(String key, double defaultValue) { return MathUtils.parseD(axis(key, defaultValue + "")); }

		String _path, configuredPath;
		public String[] lines;

		public SConfig(String path) {
			super(defaults.args);
			this._path = path;
			this.configuredPath = configuredPath(path);
			try {
				for (String line : this.lines = readConfigLines(path))
					if (line.split(setS).length > 1)
						put(line.split(setS)[0], line.split(setS)[1]);
					else put(size() + "", line.split(setS)[0]);
			} catch (Exception e) {}
		}

		public void apply(Textures textures, HashMap<Integer, String> blockNames) {
			textures.config = this;
			for (int i = 0; i < size(); i++)
				try {
					String textureName = get(i + ""), blockName = textureName.replace(paths.get(".png"), ""), texturePath = paths.get("textures") + "/" + textureName;
					if (textureName.contains(paths.get(".png")))
						Msgs.last.debug(textures.load(texturePath) + " from " + texturePath);
					blockNames.put(blockNames.size(), blockName);
				} catch (Exception e) {
					Msgs.last.warn(e);
				}
		}

		public boolean isKeyDown(Input input, String key) {
			key = key + "";
			if (orSplit(key).length > 1)
				for (String aliase : orSplit(key))
					if (isKeyDown(input, aliase))
						return true;
			return input.isKeyDown(key);
		}

		public VectorD getInputValue(Input input, String type, String control) {
			if (control.contains("_axis")) {
				VectorD a = new VectorD(MathUtils.parseI(control.split("_")[0]));
				for (int i = 0; i < a.size(); i++) {
					a.coords[i] += getInputValue(input, type, "axis" + i + "+" + (i < 3 ? orS + "right,up,forward".split(",")[i] : "")).coords[0];
					a.coords[i] -= getInputValue(input, type, "axis" + i + "-" + (i < 3 ? orS + "left,down,back".split(",")[i] : "")).coords[0];
				}
				return a;
			}
			VectorD value = new VectorD(0d);
			for (String aliase : orSplit(control))
				if (get(aliase + typeS + type) != null) {
					if (value.coords[0] == 0)
						value.coords[0] = -1d;
					if (isKeyDown(input, axis(aliase + typeS + type, "undefined") + ""))
						value.coords[0] = 1d;
				}
			return value;
		}
	}

	public static class PlayerConfig extends SConfig {
		private static final long serialVersionUID = 4605847166503076593L;

		UserController player;

		public PlayerConfig(UserController player) {
			super("players/" + player.getName() + "/player.cfg");
			this.player = player;
		}

		public void configPlayer(World world) {
			player.controls = new SConfig("players/" + player.getName() + "/controls.cfg");

			for (int i = 0; i < player.position.coords.length; i++)
				player.position.coords[i] = this.coord("position." + i, world.size.coords[i] + (i == 1 ? -2 : (i == 3 ? -0.5 : -1)));
			for (int i = 0; i < player.velocity.coords.length; i++)
				player.velocity.coords[i] = this.coord("velocity." + i, 0);
			for (int i = 0; i < player.vision.coords.length; i++)
				player.vision.coords[i] = this.coord("vision." + i, i == 3 ? 0.5 : world.size.coords[i]);
			for (int i = 0; i < player.size.coords.length; i++)
				player.size.coords[i] = this.coord("size." + i, i == 1 ? 0.9 : 0.5);
			for (int i = 0; i < player.breakDistance.coords.length; i++)
				player.breakDistance.coords[i] = this.coord("blockBreakingDistance." + i, i == 3 ? player.size.coords[i] : 5);
			for (int i = 0; i < player.rotation.coords.length; i++)
				player.rotation.coords[i] = this.coord("rotation." + i, 90);

			player.sensitivity = this.num("sensitivity", 1);

			for (int i = 0; i < player.rotation.coords.length; i++)
				player.canMoveTo[i] = this.coord("canMoveTo." + i, 90) != 0;
			for (int i = 0; i < player.rotation.coords.length; i++)
				player.moveAtLook[i] = this.coord("moveAtLook." + i, 90) != 0;

			player.freecamOnFlying = this.bool("freecamOnFlying");
			player.freecamOnSpectator = !this.bool("dontFreecamOnSpectator");
			player.jump = this.num("jump", 20);
			player.sprint = this.num("sprint", 3);
			player.speed = this.num("speed", 5);

			player.canBreakAll = this.bool("canBreakAll");
			player.lineSelector = this.bool("lineSelector");
			player.blockSelectorOff = this.bool("blockSelectorOff");
			player.onPlayerFixedSelector = this.bool("onPlayerFixedSelector");
			player.blinkingSelector = this.bool("blinkingSelector");
			player.selector = new Block(null, 0, 0, 0, 0, (int) this.num("selectorId", 21));
			player.idInHand = (int) this.num("idInHand", 1);
			player.blocksInSecond = this.num("blocksInSecond", 1);
			player.tntExplosionRadius = this.num("tntExplosionRadius", 5);
			player.collisionsInsideColliders = this.bool("collisionsInsideColliders");
			player.onCollisionMotionModifier = this.num("onCollisionMotionModifier", 0);
			player.onCollisionVelocityModifier = this.num("onCollisionVelocityModifier", 0);

			player.transperantBlocksFromOtherWorlds = this.bool("transperantBlocksFromOtherWorlds");
		}
	}

	public static class WorldConfig extends SConfig {
		private static final long serialVersionUID = -935320013479956125L;

		public WorldConfig(String path) { super(path); }

		static String[] sp = { "\n", ";", ",", "'" };

		public World loadWorld(String path) {
			String saveTxt = String.join(sp[0], lines);
			String[][][][] saveIds = new String[saveTxt.split(sp[0]).length][saveTxt.split(sp[0])[0].split(sp[1]).length][

			saveTxt.split(sp[0])[0].split(sp[1])[0].split(sp[2]).length][saveTxt.split(sp[0])[0].split(sp[1])[0].split(sp[2])[0].split(sp[3]).length];
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
			world = new World(saveIds.length, saveIds[0].length, saveIds[0][0].length, saveIds[0][0][0].length);
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
			setWorldByConfig();
			return world;
		}

		public World generateWorld() {
			SConfig cfg = getCfg();
			this.lines = cfg.lines;
			Generation[] generations = new Generation[lines.length];
			for (int i = 0; i < generations.length; i++)
				try {
					generations[i] = GenerationType.valueOf(lines[i].toUpperCase());
				} catch (Exception e) {
					final int o = i;
					generations[i] = new Generation() {
						@Override
						public Block getBlock(World world, double x, double y, double z, double w) {
							if (y < MathUtils.parseI(lines[o].split(":")[0]))
								return new Block(world, (int) x, (int) y, (int) z, (int) w, MathUtils.parseI(lines[o].split(":")[1]));
							else return world.getBlock(x, y, z, w);
						}
					};
				}
			world = new World((int) cfg.num("xSize", 0), (int) cfg.num("ySize", 0), (int) cfg.num("zSize", 0), (int) cfg.num("wSize", 0), generations);
			setWorldByConfig();
			return world;
		}

		private SConfig getCfg() {
			SConfig savesCfg = new SConfig(_path.replace(".save", ".cfg"));
			SConfig worldCfg = new SConfig("configs/world.cfg");
			return savesCfg.lines != null && savesCfg.lines.length != 0 ? savesCfg : worldCfg;
		}

		private void setWorldByConfig() {
			SConfig cfg = getCfg();
			world.blocksCollision = cfg.bool("blocksCollision");
			world.border = cfg.bool("border");
			world.clamp = cfg.bool("clamp");
			world.repeat = cfg.bool("repeat");
			world.loop = cfg.bool("loop");
		}

		World world;

		public World getWorld() {
			if (world == null)
				try {
					world = loadWorld(_path);
				} catch (Exception e) {
					world = generateWorld();
				}
			return world;
		}

		public void saveWorld(World world) {
			SConfig cfg = null;
			try {
				cfg = new SConfig(_path);
			} catch (Exception e) {}
			String text = "";
			for (int x = 0; x < world.size.coords[0]; x++, text += sp[0])
				for (int y = 0; y < world.size.coords[1]; y++, text += sp[1])
					for (int z = 0; z < world.size.coords[2]; z++, text += sp[2])
						for (int w = 0; w < world.size.coords[3]; w++, text += sp[3])
							text += world.getBlock(x, y, z, w).id;
			cfg.saveConfigText(_path, text);
		}
	}

}