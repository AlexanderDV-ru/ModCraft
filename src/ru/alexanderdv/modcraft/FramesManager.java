package ru.alexanderdv.modcraft;

import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.swing.JPanel;

import ru.alexanderdv.modcraft.Input.DisplayInput;

public class FramesManager implements Destroyable {
	public static class FrameShell implements Repaintable, Destroyable {
		private final ConcurrentSkipListMap<Integer, Object> messages = new ConcurrentSkipListMap<>();

		java.awt.Frame frame;

		public String parentName;

		public FrameShell(String parentName) { this.parentName = parentName; }

		public void init(java.awt.Frame frame) {
			if (frame != null)
				try {
					this.frame = frame;
					frame.setTitle(parentName + (!frame.getTitle().equals("") ? ": " + frame.getTitle() : ""));
					frame.setVisible(true);

					frame.setSize(400, 400);
					frame.setLocation(2000, 0);
					frame.add(new JPanel() {
						private static final long serialVersionUID = 234495029555027724L;

						@Override
						public void paintComponent(Graphics g) {
							super.paintComponent(g);
							this.setMinimumSize(frame.getSize());
							int y = 0, lineOffset = 20;
							for (Object message : messages.values())
								if (message instanceof Image)
									g.drawImage((Image) message, 0, y += lineOffset, null);
								else g.drawString(message + "", 0, y += lineOffset);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
		}

		public void println(String message) {
			for (String line : message.split("\n"))
				messages.put(messages.size(), line);
		}

		public void print(String message) {
			messages.clear();
			println(message);
		}

		@Override
		public void repaint() {
			if (frame != null)
				frame.repaint();
		}

		@Override
		public void destroy() {
			try {
				Destroyable.super.destroy();
			} catch (DestroyFailedException e) {}
			if (frame != null)
				frame.setVisible(false);
		}

		@Override
		public boolean isDestroyed() { return frame == null ? false : Destroyable.super.isDestroyed() || !frame.isVisible(); }
	}

	public final FrameShell displayFrame, debugFrame;
	public final DisplayShell display;
	public final DisplayInput input;
	private final ArrayList<FrameShell> shells = new ArrayList<>();
	public String name;

	public FramesManager(String name) {
		this.name = name;
		shells.add(this.displayFrame = new FrameShell(name));
		shells.add(this.debugFrame = new FrameShell(name));
		shells.add(this.display = new DisplayShell(name));
		this.input = new DisplayInput();
	}

	@Override
	public boolean isDestroyed() {
		for (int i = 0; i < shells.size(); i++)
			if (shells.get(i).isDestroyed())
				return true;
		return Destroyable.super.isDestroyed();
	}

	@Override
	public void destroy() {
		input.destroy();
		for (int i = shells.size() - 1; i >= 0; i--)
			shells.get(i).destroy();
	}

	public void update() {
		for (int i = 0; i < shells.size(); i++)
			shells.get(i).repaint();
	}
}