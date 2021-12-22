package ru.alexanderdv.modcraft;

import java.util.ArrayList;

import javax.security.auth.Destroyable;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public interface Input extends Destroyable {

	public boolean isKeyDown(int id);

	public default boolean isKeyDown(String key) {
		try {
			return isKeyDown((int) Double.parseDouble(key + ""));
		} catch (Exception e) {
			key = (key + "").toLowerCase().replaceAll("key[_]*", "").replace("ctrl", "control").replace("alt", "menu").replace("win", "meta").toUpperCase();
			if (key.equals("ENTER"))
				return isKeyDown(Keyboard.KEY_NUMPADENTER) || isKeyDown(Keyboard.KEY_RETURN);
			if (key.equals("CONTROL"))
				return isKeyDown(Keyboard.KEY_LCONTROL) || isKeyDown(Keyboard.KEY_RCONTROL);
			if (key.equals("MENU"))
				return isKeyDown(Keyboard.KEY_LMENU) || isKeyDown(Keyboard.KEY_RMENU);
			if (key.equals("SHIFT"))
				return isKeyDown(Keyboard.KEY_LSHIFT) || isKeyDown(Keyboard.KEY_RSHIFT);
			if (key.equals("META"))
				return isKeyDown(Keyboard.KEY_LMETA) || isKeyDown(Keyboard.KEY_RMETA);
			return isKeyDown(Keyboard.getKeyIndex(key + ""));
		}
	}

	public static class Key implements Input {
		public int id;
		public boolean downed;

		public Key(int id, boolean downed) {
			super();
			this.id = id;
			this.downed = downed;
		}

		public Key() { this(Keyboard.getEventKey(), Keyboard.isKeyDown(Keyboard.getEventKey())); }

		@Override
		public boolean isKeyDown(int id) { return this.id == id && downed; }

	}

	public static class DisplayInput implements Input {
		public static class KeysList extends ArrayList<Key> {
			private static final long serialVersionUID = 4662086084680411839L;

			public Key remove(int index) {
				try {
					return super.remove(index);
				} catch (Exception e) {
					return null;
				}
			}
		}

		final KeysList keys = new KeysList(), nextKeys = new KeysList();

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
				nextKeys.add(new Key());
			for (int id = 0; id < 128; id++)
				if (Keyboard.isKeyDown(id))
					keys.add(new Key(id, true));
		}

		public void destroy() {
			Mouse.destroy();
			Keyboard.destroy();
		}

		public void setCursorPosition(int x, int y) { Mouse.setCursorPosition(x, y); }

		public void setGrabbed(boolean grabbed) { Mouse.setGrabbed(grabbed); }

		public float getDX() { return Mouse.getDX(); }

		public float getDY() { return Mouse.getDY(); }

		public float getVolutionX() { return -getDY(); }

		public float getVolutionY() { return getDX(); }

		public Key next() { return nextKeys.remove(0); }

		@Override
		public boolean isKeyDown(int id) { return Keyboard.isKeyDown(id); }
	}
}