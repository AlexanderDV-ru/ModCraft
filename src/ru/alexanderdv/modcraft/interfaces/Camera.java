package ru.alexanderdv.modcraft.interfaces;

import ru.alexanderdv.modcraft.POV;
import ru.alexanderdv.utils.lwjgl.VerticalNormalised;

public interface Camera extends VerticalNormalised {
	public void openEyes(double width, double height, double visionDistance);

	public void pointOfView(POV p);

	public void closeEyes();
}