package ru.alexanderdv.modcraft;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Image;

import org.lwjgl.opengl.Display;

import ru.alexanderdv.modcraft.FramesManager.FrameShell;

public class DisplayShell extends FrameShell {

	public DisplayShell(String parentName) { super(parentName); }

	Canvas canvas;

	public void init(java.awt.Frame frame) {
		this.frame = frame;
		try {
			if (frame != null)
				Display.setParent((Canvas) frame.add(canvas = new Canvas() {
					private static final long serialVersionUID = -5752073716249852847L;

					@Override
					public void paint(Graphics g) {
						super.paint(g);
						//this.setMinimumSize(frame.getSize());
						int y = 0, lineOffset = 20;
						for (Object message : messages.values())
							if (message instanceof Image)
								g.drawImage((Image) message, 0, y += lineOffset, null);
							else g.drawString(message + "", 0, y += lineOffset);
					}
				}));
			Display.setTitle(frame != null ? frame.getTitle() : parentName);
			Display.create();

			Display.setVSyncEnabled(true);
			Display.setResizable(true);
			Display.setFullscreen(false);
			super.init(frame);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void repaint() {
		Display.update();
		super.repaint();
	}

	@Override
	public void destroy() {
		Display.destroy();
		super.destroy();
	}

	@Override
	public boolean isDestroyed() { return Display.isCloseRequested() || super.isDestroyed() || !Display.isCreated(); }

	public int getWidth() { return canvas != null ? canvas.getWidth() : Display.getWidth(); }

	public int getHeight() { return canvas != null ? canvas.getHeight() : Display.getHeight(); }
}