package ru.alexanderdv.modcraft.interfaces;

import ru.alexanderdv.modcraft.Block;

public interface Generation {
	Block getBlock(IWorld world, double x, double y, double z, double w);

	String generationToString();
}