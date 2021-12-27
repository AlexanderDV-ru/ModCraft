package ru.alexanderdv.modcraft.interfaces;

import ru.alexanderdv.modcraft.Block;
import ru.alexanderdv.modcraft.World;

public interface Generation {
	Block getBlock(World world, double x, double y, double z, double w);

	String generationToString();
}