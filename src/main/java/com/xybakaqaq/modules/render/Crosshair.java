package com.xybakaqaq.modules.render;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.settings.ColorSetting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

// Colors the crosshair by whatever you are aiming at: a player (red), a hostile mob, an animal, or
// the default color when aiming at nothing. Drawn by com.xybakaqaq.mixin.HudCrosshairMixin,
// which replaces the vanilla crosshair while this module is enabled.
public final class Crosshair extends Module {

	private final ColorSetting playerColor = new ColorSetting("Player", 0xFFFF4040);
	private final ColorSetting mobColor = new ColorSetting("Mobs", 0xFFFFA030);
	private final ColorSetting animalColor = new ColorSetting("Animals", 0xFF54C85A);
	private final ColorSetting defaultColor = new ColorSetting("Default", 0xFFFFFFFF);

	public Crosshair() {
		super("Crosshair", Category.RENDER, "Colors the crosshair by the entity you aim at.", InputConstants.UNKNOWN.getValue());
	}

	private int colorFor(Entity entity) {
		if (entity instanceof Player) {
			return playerColor.getColor();
		}
		if (entity instanceof Enemy) {
			return mobColor.getColor();
		}
		if (entity instanceof Animal) {
			return animalColor.getColor();
		}
		return defaultColor.getColor();
	}

	// Draws the colored crosshair at the screen center. Called from the HUD mixin.
	public static void render(GuiGraphicsExtractor graphics) {
		Crosshair module = ModuleManager.get(Crosshair.class);
		if (module == null) {
			return;
		}
		graphics.nextStratum();
		int color = module.colorFor(Minecraft.getInstance().crosshairPickEntity);
		int cx = graphics.guiWidth() / 2;
		int cy = graphics.guiHeight() / 2;
		// black outline, then the colored plus on top
		graphics.fill(cx - 1, cy - 7, cx + 2, cy + 8, 0xB0000000);
		graphics.fill(cx - 7, cy - 1, cx + 8, cy + 2, 0xB0000000);
		graphics.fill(cx, cy - 6, cx + 1, cy + 7, color);
		graphics.fill(cx - 6, cy, cx + 7, cy + 1, color);
	}
}
