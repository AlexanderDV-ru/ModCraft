package ru.alexanderdv.modcraft.configs;

import java.util.HashMap;

import ru.alexanderdv.modcraft.Input;
import ru.alexanderdv.modcraft.Textures;
import ru.alexanderdv.utils.MathUtils;
import ru.alexanderdv.utils.VectorD;
import ru.alexanderdv.utils.MessageSystem.Msgs;

public class SConfig extends Config<String> {
	private static final long serialVersionUID = -165168986796274757L;

	@Override
	public String get(Object key) { return super.get(key) != null ? super.get(key) : "undefined"; }

	public String get(Object key, String defaultValue) { return bool(key) ? get(key) : defaultValue; }

	public boolean toBool(String value) { return !value.matches("false|undefined|default|null|[0.]+|not|no|n|[ ]*"); }

	public double toNum(String value) { return MathUtils.parseD(value); }

	public boolean bool(Object key) { return toBool(get(key)); }

	public double num(Object key, double defaultValue) { return toNum(get(key, defaultValue + "")); }

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