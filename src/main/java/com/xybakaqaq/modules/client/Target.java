package com.xybakaqaq.modules.client;

import org.lwjgl.glfw.GLFW;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.settings.BooleanSetting;

/**
 * Shared target configuration: which entity kinds count as a valid target (players / animals /
 * mobs), plus whether to include invisible or dying entities. Read by {@link
 * com.xybakaqaq.utils.TargetUtil}; other features (combat, target HUD, …) can target through it.
 */
public final class Target extends Module {

	private final BooleanSetting players = new BooleanSetting("Players", true);
	private final BooleanSetting animals = new BooleanSetting("Animals", false);
	private final BooleanSetting mobs = new BooleanSetting("Mobs", true);
	private final BooleanSetting invisibles = new BooleanSetting("Invisibles", false);
	private final BooleanSetting dead = new BooleanSetting("Dead", false);

	public Target() {
		super("Target", Category.CLIENT, "Configures which entities count as targets.", GLFW.GLFW_KEY_UNKNOWN, true);
	}

	public boolean players() {
		return players.isEnabled();
	}

	public boolean animals() {
		return animals.isEnabled();
	}

	public boolean mobs() {
		return mobs.isEnabled();
	}

	public boolean invisibles() {
		return invisibles.isEnabled();
	}

	public boolean dead() {
		return dead.isEnabled();
	}
}
