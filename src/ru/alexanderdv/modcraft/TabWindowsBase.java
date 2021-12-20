package ru.alexanderdv.modcraft;

import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.swing.JPanel;

public class TabWindowsBase implements Repaintable, Destroyable {
	public static class TabWindow implements Repaintable, Destroyable {
		private final ConcurrentSkipListMap<Integer, Object> messages = new ConcurrentSkipListMap<>();

		java.awt.Frame window;

		public String parentName;

		public TabWindow(String parentName) { this.parentName = parentName; }

		public void init(java.awt.Frame window) {
			if (window != null)
				try {
					this.window = window;
					window.setTitle(parentName + (!window.getTitle().equals("") ? ": " + window.getTitle() : ""));
					window.setVisible(true);

					window.setSize(400, 80);
					window.setLocation(2000, 0);
					window.add(new JPanel() {
						private static final long serialVersionUID = 234495029555027724L;

						@Override
						public void paintComponent(Graphics g) {
							super.paintComponent(g);
							this.setMinimumSize(window.getSize());
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

		@Override
		public void repaint() {
			if (window != null)
				window.repaint();
		}

		@Override
		public void destroy() {
			try {
				Destroyable.super.destroy();
			} catch (DestroyFailedException e) {}
			if (window != null)
				window.setVisible(false);
		}

		@Override
		public boolean isDestroyed() { return window == null ? false : Destroyable.super.isDestroyed() || !window.isVisible(); }
	}

	public final TabWindow frame, output;
	public final DisplayTabWindow display;
	public final Input input;
	private final ArrayList<TabWindow> tabWindows = new ArrayList<>();
	public String name;

	public TabWindowsBase(String name) {
		this.name = name;
		tabWindows.add(this.frame = new TabWindow(name));
		tabWindows.add(this.output = new TabWindow(name));
		tabWindows.add(this.display = new DisplayTabWindow(name));
		this.input = new Input();
	}

	public void println(Object message) { output.messages.put(output.messages.size(), message); }

	public void print(Object message) {
		output.messages.clear();
		println(message);
	}

	@Override
	public boolean isDestroyed() {
		for (int i = 0; i < tabWindows.size(); i++)
			if (tabWindows.get(i).isDestroyed())
				return true;
		return Destroyable.super.isDestroyed();
	}

	@Override
	public void destroy() {
		input.destroy();
		for (int i = tabWindows.size() - 1; i >= 0; i--)
			tabWindows.get(i).destroy();
	}

	@Override
	public void repaint() {
		for (int i = 0; i < tabWindows.size(); i++)
			tabWindows.get(i).repaint();
	}
}