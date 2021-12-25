package ru.alexanderdv.modcraft;

public class WorldEdit {
	World world;

	public WorldEdit(World world) { this.world = world; }

	public void createExplosion(double ex, double ey, double ez, double radius) { sphere(ex, ey, ez, radius, 0, false); }

	public void sphere(double ex, double ey, double ez, double radius, int id, boolean anti) {
		for (int x = (int) (ex - radius); x < ex + radius; x++)
			for (int y = (int) (ey - radius); y < ey + radius; y++)
				for (int z = (int) (ez - radius); z < ez + radius; z++)
					if ((Math.sqrt(Math.pow(ex - x, 2) + Math.pow(ey - y, 2) + Math.pow(ez - z, 2)) < radius) == !anti)
						world.setBlock(x, y, z, id);
	}
}