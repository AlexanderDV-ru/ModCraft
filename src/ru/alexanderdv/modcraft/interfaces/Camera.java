package ru.alexanderdv.modcraft.interfaces;

import ru.alexanderdv.modcraft.POV;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public interface Camera extends VerticalNormalised {
	public void openEyes(float fov, float width, float height, float visionDistance);

	public void pointOfView(POV p);

	public void closeEyes();
}