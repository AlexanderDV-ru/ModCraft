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

	public final int KEY_END = Keyboard.KEY_END;
	public final int KEY_ESCAPE = Keyboard.KEY_ESCAPE;
	public final int KEY_SPACE = Keyboard.KEY_SPACE;
	public final int KEY_F = Keyboard.KEY_F;
	public final int KEY_W = Keyboard.KEY_W;
	public final int KEY_A = Keyboard.KEY_A;
	public final int KEY_S = Keyboard.KEY_S;
	public final int KEY_D = Keyboard.KEY_D;
	public final int KEY_LCONTROL = Keyboard.KEY_LCONTROL;

	final KeysList stateKeys = new KeysList(), keys = new KeysList();

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

	public boolean isKeyDown(int key) { return Keyboard.isKeyDown(key); }

	public boolean next() { return Keyboard.next(); }
}