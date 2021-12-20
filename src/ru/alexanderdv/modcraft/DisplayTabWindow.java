package ru.alexanderdv.modcraft;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;

import org.lwjgl.opengl.Display;

import ru.alexanderdv.modcraft.TabWindowsBase.TabWindow;

public class DisplayTabWindow extends TabWindow {

	public DisplayTabWindow(String parentName) { super(parentName); }

	public void init(java.awt.Frame window) {
		super.init(window);
		try {
			if (window != null)
				Display.setParent((Canvas) window.add(new Canvas() {
					private static final long serialVersionUID = -5752073716249852847L;

					@Override
					public void paint(Graphics g) {
						super.paint(g);
						this.setMinimumSize(new Dimension((int) window.getSize().getWidth(), (int) window.getSize().getHeight()));
					}
				}));
			Display.setTitle(window != null ? window.getTitle() : parentName);
			Display.create();

			Display.setVSyncEnabled(true);
			Display.setResizable(true);
			Display.setFullscreen(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void repaint() {
		super.repaint();
		Display.update();
	}

	@Override
	public void destroy() {
		Display.destroy();
		super.destroy();
	}

	@Override
	public boolean isDestroyed() { return Display.isCloseRequested() || super.isDestroyed() || !Display.isCreated(); }

	public int getWidth() { return Display.getWidth(); }

	public int getHeight() { return Display.getHeight(); }
}