package com.xybakaqaq.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// Shared combat-target resolver driven by both aiming and attacking: the entity under the
// crosshair updates the target every frame, and #onAttack records a hit (so a target you
// struck without holding the aim still counts). The remembered target is kept while alive, and
// reported by #resolve when within the queried range. Used by the target HUD and Autoblock.
public final class CombatTarget {

	private static LivingEntity lastTarget;

	private CombatTarget() {
	}

	// Records the entity the player just attacked.
	public static void onAttack(Entity entity) {
		if (entity instanceof LivingEntity living) {
			lastTarget = living;
		}
	}


	public static LivingEntity resolve(double maxBlocks) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return null;
		}
		// aiming feeds the target too (the crosshair pick is already limited to the player's reach)
		if (mc.crosshairPickEntity instanceof LivingEntity aimed && aimed != mc.player
				&& aimed.isAlive() && !aimed.isRemoved()) {
			lastTarget = aimed;
		}
		LivingEntity target = lastTarget;
		if (target == null || !target.isAlive() || target.isRemoved()) {
			lastTarget = null;
			return null;
		}
		// keep the reference when out of range (so a closer query / re-approach still sees it)
		return mc.player.distanceTo(target) <= maxBlocks ? target : null;
	}


	public static boolean has(double maxBlocks) {
		return resolve(maxBlocks) != null;
	}
}
