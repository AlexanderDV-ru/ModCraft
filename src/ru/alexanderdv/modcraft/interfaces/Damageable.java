package ru.alexanderdv.modcraft.interfaces;

public interface Damageable {

	public default void heal(double count) { setHealth(getHealth() + Math.abs(count)); }

	public default void damage(double count) { setHealth(getHealth() - Math.abs(count)); }

	void setHealth(double count);

	double getHealth();

	void setMaxHealth(double count);

	double getMaxHealth();

	void setAutoHealing(double count);

	double getAutoHealing();

	void death();
}
