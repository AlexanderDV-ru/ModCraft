package ru.alexanderdv.modcraft;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.imageio.ImageIO;

import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.MessageSystem.Msgs;
import ru.alexanderdv.utils.VectorD;

public class Config<Type> extends HashMap<String, Type> {
	private static final long serialVersionUID = -1536270919370232799L;

	public static Config<String> defaults;

	public String rawReplaceR = "([ \"])|([/][*][^/]*[*][/])|(([/][/]|[#])[^\n]*)", newElementS = "\n", orS = "||", typeS = "_", setS = ":";
	public String assetsS = "assets", playersS = "players", configS = "config", texturesS = "textures", configsS = "configs", controlsS = "controls", cfgExtS = ".cfg", textureExtS = ".png";
	public String assetsPath, configPath = configS + "/";
	public final String[] args;

	public Config(String[] args) {
		this.args = args;
		assetsPath = assetsS + "/" + args[0] + "/";
		if (defaults != null)
			try {
				String[] lines = readConfigText("configs/configs.cfg").split(newElementS);
				rawReplaceR = lines[0];
				newElementS = unshieldSpecial(lines[1]);
				orS = lines[2];
				typeS = lines[3];
				setS = lines[4];
			} catch (Exception e) {
				Msgs.last.debug(e);
			}
	}

	public String shieldSpecial(String string) { return "[" + String.join("][", orS.split("")) + "]"; }

	public String unshieldSpecial(String specialString) { return specialString.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t"); }

	public String[] orSplit(String control) { return control.split(shieldSpecial(orS)); }

	// Args

	static String args(String... args) { return String.join(" ", args); }

	public static boolean hasArg(String name, String... args) { return args(args).contains(name); }

	public static String getArg(String name, String... args) { return args(args).split(name)[1].split(" ")[1]; }

	public static String getArg(int number, String... args) { return args(args).split(" ")[number]; }

	public static double getNum(String name, String... args) { return Double.parseDouble(getArg(name, args)); }

	public static int getInt(String name, String... args) { return (int) getNum(name, args); }

	// Read

	static Charset defaultCharset = Charset.forName("UTF-8");

	public InputStream getStream(String path) {
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

	public InputStream getConfigStream(String path) {
		try {
			return getStream(configPath + path);
		} catch (Exception e3) {
			try {
				return getStream(assetsPath + path);
			} catch (Exception e) {
				try {
					return getStream(assetsPath + configsS + "/" + path.split("/")[path.split("/").length - 1]);
				} catch (Exception e2) {
					throw new RuntimeException("Error with reading from " + path, e3.getCause());
				}
			}
		}
	}

	public BufferedImage readConfigImage(String path) {
		try {
			return ImageIO.read(getConfigStream(path));
		} catch (Exception e2) {
			throw new RuntimeException("Error with reading from " + path, e2);
		}
	}

	public String readConfigText(String path) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(getConfigStream(path), defaultCharset));
			String lines = "";
			for (String line; (line = reader.readLine()) != null;)
				lines += line + "\n";
			return lines;
		} catch (Exception e2) {
			throw new RuntimeException("Error with reading from " + path, e2);
		}
	}

	public String[] readConfigLines(String path) { return readConfigText(path).replaceAll(rawReplaceR, "").split(newElementS); }

	public static class SConfig extends Config<String> {
		private static final long serialVersionUID = -165168986796274757L;

		String path;

		@Override
		public String get(Object key) { return super.get(key) != null ? super.get(key) : "undefined"; }

		public boolean bool(Object key) { return !get(key).matches("false|undefined|[0.]+|not|no|n|[ ]*"); }

		public double num(Object key) { return MathUtils.parseD(get(key)); }

		public SConfig(String path) {
			super(defaults.args);
			for (String line : readConfigLines(this.path = path))
				if (line.split(setS).length > 1)
					put(line.split(setS)[0], line.split(setS)[1]);
				else put(size() + "", line.split(setS)[0]);
		}

		public void apply(Textures textures, HashMap<Integer, String> blockNames) {
			textures.config = this;
			for (int i = 0; i < size(); i++)
				try {
					String textureName = get(i + ""), blockName = textureName.replace(textureExtS, ""), texturePath = texturesS + "/" + textureName;
					if (textureName.contains(textureExtS))
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
					a.coords[i] += getInputValue(input, type, "axis" + i + "+" + (i < 5 ? orS + "axis" + "xyzwt".charAt(i) + "+" : "") + (i < 3 ? orS + "right,up,forward".split(",")[i] : "")).coords[0];
					a.coords[i] -= getInputValue(input, type, "axis" + i + "-" + (i < 5 ? orS + "axis" + "xyzwt".charAt(i) + "-" : "") + (i < 3 ? orS + "left,down,back".split(",")[i] : "")).coords[0];
				}
				return a;
			}
			VectorD value = new VectorD(0d);
			for (String aliase : orSplit(control))
				if (get(aliase + typeS + type) != null) {
					if (value.coords[0] == 0)
						value.coords[0] = -1d;
					if (isKeyDown(input, get(aliase + typeS + type) + ""))
						value.coords[0] = 1d;
				}
			return value;
		}
	}
}