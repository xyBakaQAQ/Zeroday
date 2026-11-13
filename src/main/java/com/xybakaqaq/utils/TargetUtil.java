package com.xybakaqaq.utils;

import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.client.Target;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

// Entity targeting helper driven by the Target module's settings. #isValid filters a
// single entity; #getNearest finds the closest valid target around the player.
public final class TargetUtil {

	private TargetUtil() {
	}


	public static boolean isValid(Entity entity) {
		if (!(entity instanceof LivingEntity living)) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		if (living == mc.player) {
			return false;
		}

		Target target = ModuleManager.get(Target.class);
		boolean players = target == null || target.players();
		boolean animals = target != null && target.animals();
		boolean mobs = target == null || target.mobs();
		boolean invisibles = target != null && target.invisibles();
		boolean dead = target != null && target.dead();

		if (!dead && living.isDeadOrDying()) {
			return false;
		}
		if (!invisibles && living.isInvisible()) {
			return false;
		}

		if (living instanceof Player) {
			return players;
		}
		if (living instanceof Enemy) {
			return mobs;
		}
		if (living instanceof Animal) {
			return animals;
		}
		return false;
	}


	public static LivingEntity getNearest(double range) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return null;
		}
		LivingEntity best = null;
		double bestDistance = range;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!isValid(entity)) {
				continue;
			}
			double distance = mc.player.distanceTo(entity);
			if (distance <= bestDistance) {
				bestDistance = distance;
				best = (LivingEntity) entity;
			}
		}
		return best;
	}
}
