package ru.alexanderdv.modcraft.configs;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.imageio.ImageIO;

import ru.alexanderdv.utils.ExceptionsHandler;

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

	public OutputStream getOutputStream(String path) throws IOException {
		path = configuredPath(path);
		File f = new File(savesPath + path);
		ExceptionsHandler.tryCatchVoid(() -> f.getParentFile().mkdirs());
		ExceptionsHandler.tryCatchVoid(() -> f.createNewFile());
		return Files.newOutputStream(Paths.get(f.getAbsolutePath()));
	}

	public void saveConfigText(String path, String text) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getOutputStream(path), defaultCharset));
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

}