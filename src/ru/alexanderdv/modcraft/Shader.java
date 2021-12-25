package ru.alexanderdv.modcraft;

import java.util.Random;

import ru.alexanderdv.utils.lwjgl.Timed;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public interface Shader extends Timed {

	public static class FlickingShader implements Shader, VerticalNormalised {
		double overlayTexturesFlickingModifier = 1, currentFlickingOffset = 0;
		Random random = new Random();

		@Override
		public void render() { glTranslated(currentFlickingOffset * 0, currentFlickingOffset * 0, currentFlickingOffset * 1); }

		@Override
		public void update() { currentFlickingOffset = (random.nextDouble() - 0.5d) / 1001d * overlayTexturesFlickingModifier; }
	}// TODO 0.5 kinematics divide world picture to 2, mind this bug how non strict
		// blocks
}