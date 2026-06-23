package com.xybakaqaq.modules.movement;

import org.lwjgl.glfw.GLFW;

import com.xybakaqaq.event.features.TickEvent;
import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;

/**
 * Always-sprint: each tick, holds down the vanilla sprint key so the game's own sprint logic
 * runs — no custom movement code. The {@code isDown()} guard keeps it pressed once (correct for
 * both hold-to-sprint and toggle-sprint options).
 */
public final class Sprint extends Module {

	public Sprint() {
		super("Sprint", Category.MOVEMENT, "Hold the sprint key automatically.", GLFW.GLFW_KEY_UNKNOWN, true);
	}

	@Override
	public void onTick(TickEvent event) {
		if (mc.player != null && !mc.options.keySprint.isDown()) {
			mc.options.keySprint.setDown(true);
		}
	}

	@Override
	protected void onDisable() {
		mc.options.keySprint.setDown(false);
	}
}
