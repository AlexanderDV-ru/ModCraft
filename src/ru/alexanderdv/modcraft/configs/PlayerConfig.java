package ru.alexanderdv.modcraft.configs;

import ru.alexanderdv.modcraft.Block;
import ru.alexanderdv.modcraft.Controller;
import ru.alexanderdv.modcraft.DamageableUserController;
import ru.alexanderdv.modcraft.World;
import ru.alexanderdv.modcraft.Controller.UserController;

public class PlayerConfig extends SConfig {
	private static final long serialVersionUID = 4605847166503076593L;

	Controller controller;

	public PlayerConfig(Controller controller) {
		super("players/" + controller.getName() + "/player.cfg");
		this.controller = controller;
	}

	public void configPlayer(World world) {
		if (controller instanceof UserController)
			((UserController) controller).controls = new SConfig("players/" + controller.getName() + "/controls.cfg");

		for (int i = 0; i < controller.position.coords.length; i++)
			controller.position.coords[i] = this.coord("position." + i, world.size.coords[i] + (i == 1 ? -2 : (i == 3 ? -0.5 : -1)));
		for (int i = 0; i < controller.velocity.coords.length; i++)
			controller.velocity.coords[i] = this.coord("velocity." + i, 0);
		for (int i = 0; i < controller.vision.coords.length; i++)
			controller.vision.coords[i] = this.coord("vision." + i, i == 3 ? 0.5 : world.size.coords[i]);
		for (int i = 0; i < controller.size.coords.length; i++)
			controller.size.coords[i] = this.coord("size." + i, i == 1 ? 0.9 : 0.5);
		for (int i = 0; i < controller.breakDistance.coords.length; i++)
			controller.breakDistance.coords[i] = this.coord("blockBreakingDistance." + i, i == 3 ? controller.size.coords[i] : 5);
		for (int i = 0; i < controller.rotation.coords.length; i++)
			controller.rotation.coords[i] = this.coord("rotation." + i, 90);

		controller.sensitivity = this.num("sensitivity", 1);

		for (int i = 0; i < controller.canMoveTo.length; i++)
			controller.canMoveTo[i] = toBool(this.axis("canMoveTo." + i, "true"));
		for (int i = 0; i < controller.moveAtLook.length; i++)
			controller.moveAtLook[i] = toBool(this.axis("moveAtLook." + i, i % 2 == 0 ? "true" : "false"));

		controller.jump = this.num("jump", 20);
		controller.sprint = this.num("sprint", 3);
		controller.speed = this.num("speed", 5);

		if (controller instanceof UserController) {
			UserController controller = ((UserController) this.controller);
			controller.freecamOnFlying = this.bool("freecamOnFlying");
			controller.freecamOnSpectator = !this.bool("dontFreecamOnSpectator");
			controller.canBreak = (this.bool("canBreakAll") ? "all" : (!this.get("canBreakAll").equals("") ? "default" : "")) + "," + this.get("canBreak");
			controller.lineSelector = this.bool("lineSelector");
			controller.blockSelectorOff = this.bool("blockSelectorOff");
			controller.onPlayerFixedSelector = this.bool("onPlayerFixedSelector");
			controller.blinkingSelector = this.bool("blinkingSelector");
			controller.selector = new Block(0, 0, 0, 0, (int) this.num("selectorId", 21));
			controller.tntExplosionRadius = this.num("tntExplosionRadius", 5);
			controller.transperantBlocksFromOtherWorlds = this.bool("transperantBlocksFromOtherWorlds");
		}

		controller.idInHand = (int) this.num("idInHand", 1);
		controller.blocksInSecond = this.num("blocksInSecond", 1);
		controller.collisionsInsideColliders = this.bool("collisionsInsideColliders");
		controller.onCollisionMotionModifier = this.num("onCollisionMotionModifier", 0);
		controller.onCollisionVelocityModifier = this.num("onCollisionVelocityModifier", 0);

		if (controller instanceof DamageableUserController) {
			DamageableUserController controller = ((DamageableUserController) this.controller);
			controller.setMaxHealth(this.num("health", 20));
			controller.setHealth(controller.getMaxHealth());
			controller.setAutoHealing(this.num("autohealing", 0.3));
			for (int i = 0; i < controller.spawnPosition.coords.length; i++)
				controller.spawnPosition.coords[i] = this.coord("position." + i, world.size.coords[i] + (i == 1 ? -2 : (i == 3 ? -0.5 : -1)));
		}
	}
}