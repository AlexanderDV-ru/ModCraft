package ru.alexanderdv.modcraft;

import java.util.ArrayList;

import javax.security.auth.Destroyable;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Input implements Destroyable {
	public static class KeysList extends ArrayList<String> {
		private static final long serialVersionUID = 4662086084680411839L;

		public String remove(int index) {
			try {
				return super.remove(index);
			} catch (Exception e) {
				return null;
			}
		}
	}

	final KeysList stateKeys = new KeysList(), keys = new KeysList();

	double keyboardTune = 1 / 5d, mouseTune = 1 / 10d;

	public void init() {
		try {
			Keyboard.create();
			Mouse.create();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void update() {
		Mouse.updateCursor();
		while (Keyboard.next())
			if (Keyboard.getEventKeyState())
				stateKeys.add(Keyboard.getKeyName(Keyboard.getEventKey()));
		for (int id = 0; id < 128; id++)
			if (Keyboard.isKeyDown(id))
				keys.add(Keyboard.getKeyName(id));
	}

	public void destroy() {
		Mouse.destroy();
		Keyboard.destroy();
	}

	public void setCursorPosition(int x, int y) { Mouse.setCursorPosition(x, y); }

	public void setGrabbed(boolean grabbed) { Mouse.setGrabbed(grabbed); }

	public float getDX() { return Mouse.getDX(); }

	public float getDY() { return Mouse.getDY(); }

	public boolean next() { return Keyboard.next(); }

	public boolean isKeyDown(Object key) {
		try {
			return Keyboard.isKeyDown((int) Double.parseDouble(key + ""));
		} catch (Exception e) {
			key = (key + "").toLowerCase().replaceAll("key[_]*", "").replace("ctrl", "control").replace("alt", "menu").replace("win", "meta").toUpperCase();
			if (key.equals("ENTER"))
				return Keyboard.isKeyDown(Keyboard.KEY_NUMPADENTER) || Keyboard.isKeyDown(Keyboard.KEY_RETURN);
			if (key.equals("CONTROL"))
				return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
			if (key.equals("MENU"))
				return Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
			if (key.equals("SHIFT"))
				return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
			if (key.equals("META"))
				return Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA);
			return Keyboard.isKeyDown(Keyboard.getKeyIndex(key + ""));
		}
	}
}