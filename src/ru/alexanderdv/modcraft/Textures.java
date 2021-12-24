package ru.alexanderdv.modcraft;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import ru.alexanderdv.utils.ExceptionsHandler;

public class Textures {
	public static class TextureException extends RuntimeException {
		private static final long serialVersionUID = 8154035720461452175L;

		public TextureException(String message, String path, Throwable cause) { super(message + " (In path " + path + ")", cause); }
	}

	public Config<String> config;

	private HashMap<String, Integer> idMap = new HashMap<>();
	public HashMap<String, ByteBuffer> bufferMap = new HashMap<>();
	public HashMap<String, int[]> pixelsMap = new HashMap<>();

	public int use(String path) { return ExceptionsHandler.tryCatchReturn(() -> get(path), (e) -> load(path)); }

	public int get(String path) throws TextureException {
		if (this.idMap.containsKey(path))
			return ((Integer) this.idMap.get(path)).intValue();
		else throw new TextureException("Not loaded! ", path, null);
	}

	public int load(String path) throws TextureException { return load(path, 9728); }

	public int load(String path, int mode) throws TextureException {
		if (this.idMap.containsKey(path))
			throw new TextureException("Already loaded! ", path, null);
		IntBuffer ib = BufferUtils.createIntBuffer(1);
		ib.clear();
		GL11.glGenTextures(ib);
		int id = ib.get(0);
		this.idMap.put(path, id);

		GL11.glBindTexture(3553, id);

		GL11.glTexParameteri(3553, 10241, mode);
		GL11.glTexParameteri(3553, 10240, mode);

		BufferedImage img = config.readConfigImage(path);
		int w = img.getWidth();
		int h = img.getHeight();

		if (!Config.hasArg("-dontAutoEqualSides", config.args))
			w = h = Math.min(w, h);
		// TODO add animations, cuting, scaling, coloring, models, layers

		ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
		int[] rawPixels = new int[w * h];
		img.getRGB(0, 0, w, h, rawPixels, 0, w);
		for (int i = 0; i < rawPixels.length; ++i) {
			int a = rawPixels[i] >> 24 & 0xFF;
			int r = rawPixels[i] >> 16 & 0xFF;
			int g = rawPixels[i] >> 8 & 0xFF;
			int b = rawPixels[i] & 0xFF;

			rawPixels[i] = (a << 24 | b << 16 | g << 8 | r);
		}
		this.pixelsMap.put(path, rawPixels);
		pixels.asIntBuffer().put(rawPixels);
		this.bufferMap.put(path, pixels);
		GLU.gluBuild2DMipmaps(3553, 6408, w, h, 6408, 5121, pixels);
		return id;
	}
}