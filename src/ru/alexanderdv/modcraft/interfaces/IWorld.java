package ru.alexanderdv.modcraft.interfaces;

import java.io.Serializable;

import ru.alexanderdv.modcraft.Block;
import ru.alexanderdv.modcraft.PhysicalPOV.Collider;
import ru.alexanderdv.modcraft.PhysicalPOV.PhysicalEnviroment;
import ru.alexanderdv.utils.VectorD;

public interface IWorld extends Serializable, PhysicalEnviroment {
	public Block getBlock(double x, double y, double z, double w);

	public default Block getBlock(VectorD position) { return getBlock(position.getX(), position.getY(), position.getZ(), position.getW()); };

	public void setBlocks(double x, double y, double z, double w, double x2, double y2, double z2, double w2, int id);

	public default void setBlock(double x, double y, double z, double w, int id) { setBlocks(x, y, z, w, x, y, z, w, id); }

	public Collider getCollider();

	public boolean[] isNeedHide(double x, double y, double z, double w);

	public boolean isRepeat();

	public VectorD getSize();
}