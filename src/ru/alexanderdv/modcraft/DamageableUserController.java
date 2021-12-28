package ru.alexanderdv.modcraft;

import ru.alexanderdv.modcraft.Controller.UserController;
import ru.alexanderdv.modcraft.interfaces.Damageable;
import ru.alexanderdv.modcraft.interfaces.IWorld;
import ru.alexanderdv.utils.VectorD;

public class DamageableUserController extends UserController implements Damageable {
	public DamageableUserController(String name) { super(name); }

	public VectorD spawnPosition = new VectorD(position.size());
	private double health, spawnHealth, autohealing;

	@Override
	public void damage(double count) {
		if (collision.disabled)
			return;
		Damageable.super.damage(count);
		if (health <= 0)
			death();
	}

	@Override
	public void setHealth(double count) { this.health = Math.min(count, getMaxHealth()); }

	@Override
	public double getHealth() { return health; }

	@Override
	public void setMaxHealth(double count) { this.spawnHealth = count; }

	@Override
	public double getMaxHealth() { return spawnHealth; }

	@Override
	public void setAutoHealing(double count) { autohealing = count; }

	@Override
	public double getAutoHealing() { return autohealing; }

	@Override
	public void death() {
		for (int i = 0; i < position.coords.length; i++)
			position.coords[i] = spawnPosition.coords[i];
		setHealth(spawnHealth);
	}

	@Override
	public String rightButtonDown(IWorld world, double x, double y, double z, double w, VectorD lookDir) {
		if (world.getBlock(x, y, z, w).getHeal() != 0) {
			heal(world.getBlock(x, y, z, w).getHeal());
			world.setBlock(x, y, z, w, 0);
			return "healblockuse " + x + " " + y + " " + z + " " + w;
		}
		return super.rightButtonDown(world, x, y, z, w, lookDir);
	}
}
