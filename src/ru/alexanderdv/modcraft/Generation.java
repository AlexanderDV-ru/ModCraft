package ru.alexanderdv.modcraft;

public interface Generation {
	Block getBlock(World world, double x, double y, double z, double w);

	String generationToString();
}