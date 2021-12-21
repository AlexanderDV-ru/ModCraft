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

import ru.alexanderdv.utils.MessageSystem.Msgs;
import ru.alexanderdv.utils.VectorD;

public class Custom {
	public static Custom defaults;
	public String rawReplaceR = "([ \"])|([/][*][^/]*[*][/])|(([/][/]|[#])[^\n]*)", newElementS = "\n", orS = "||", typeS = "_";
	public HashMap<String, String> controls = new HashMap<>();
	public String assetsS = "assets", playersS = "players", customS = "custom", texturesS = "textures", configsS = "configs", controlsS = "controls", cfgExtS = ".cfg", textureExtS = ".png";
	public String assetsPath, customPath = customS + "/";
	public final String[] args;

	public Custom(String[] args) {
		this.args = args;
		assetsPath = assetsS + "/" + args[0] + "/";
		if (defaults != null)
			try {
				String[] lines = readTextCustom("configs/configs.cfg").split(newElementS);
				rawReplaceR = lines[0];
				newElementS = unshieldSpecial(lines[1]);
				orS = lines[2];
				typeS = lines[3];
			} catch (Exception e) {
				Msgs.last.debug(e);
			}
	}

	public String shieldSpecial(String string) { return "[" + String.join("][", orS.split("")) + "]"; }

	public String unshieldSpecial(String specialString) { return specialString.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t"); }

	public String[] orSplit(String control) { return control.split(shieldSpecial(orS)); }

	public boolean isKeyDown(Input input, String key) {
		key = key + "";
		if (orSplit(key).length > 1)
			for (String aliase : orSplit(key))
				if (isKeyDown(input, aliase))
					return true;
		return input.isKeyDown(key);
	}

	public boolean isControl(Input input, String type, String control) {
		for (String aliase : orSplit(control))
			if (isKeyDown(input, controls.get(aliase + typeS + type)))
				return true;
		for (String aliase : orSplit(control))
			if (isKeyDown(input, controls.get(aliase)))
				return true;
		return false;
	}

	public VectorD getAxis(Input input, int size) {
		VectorD axis = new VectorD(size);
		for (int i = 0; i < size; i++) {
			axis.coords[i] += isControl(input, "when", "axis" + i + "+" + (i < 5 ? orS + "axis" + "xyzwt".split("")[i] + "+" : "") + (i < 3 ? orS + "right,up,forward".split(",")[i] : "")) ? 1 : 0;
			axis.coords[i] -= isControl(input, "when", "axis" + i + "-" + (i < 5 ? orS + "axis" + "xyzwt".split("")[i] + "-" : "") + (i < 3 ? orS + "left,down,back".split(",")[i] : "")) ? 1 : 0;
		}
		return axis;
	}

	public Custom(String path) {
		this(defaults.args);
		for (String line : readCfgCustom(path))
			controls.put(line.split(":")[0], line.split(":")[1]);
	}

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
				InputStream stream = Custom.class.getResourceAsStream("/" + path);
				stream.available();
				return stream;
			} catch (Exception e2) {
				throw new RuntimeException("Error with reading from " + path, e2);
			}
		}
	}

	public InputStream getStreamCustom(String path) {
		try {
			return getStream(customPath + path);
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

	public BufferedImage readImageCustom(String path) {
		try {
			return ImageIO.read(getStreamCustom(path));
		} catch (Exception e2) {
			throw new RuntimeException("Error with reading from " + path, e2);
		}
	}

	public String readTextCustom(String path) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(getStreamCustom(path), defaultCharset));
			String lines = "";
			for (String line; (line = reader.readLine()) != null;)
				lines += line + "\n";
			return lines;
		} catch (Exception e2) {
			throw new RuntimeException("Error with reading from " + path, e2);
		}
	}

	public String[] readCfgCustom(String path) { return readTextCustom(path).replaceAll(rawReplaceR, "").split(newElementS); }

	public void loadBlocksWithTexturesTo(Textures textures, HashMap<Integer, String> blockNames) {
		textures.custom = this;
		for (String line : readCfgCustom(configsS + "/" + texturesS + cfgExtS))
			try {
				String textureName = line.replace(" ", ""), blockName = textureName.replace(textureExtS, ""), texturePath = texturesS + "/" + textureName;
				if (textureName.contains(textureExtS))
					Msgs.last.debug(textures.load(texturePath) + " from " + texturePath);
				blockNames.put(blockNames.size(), blockName);
			} catch (Exception e) {
				Msgs.last.warn(e);
			}
	}
}