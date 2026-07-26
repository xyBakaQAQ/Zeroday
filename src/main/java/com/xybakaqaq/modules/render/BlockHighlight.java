package com.xybakaqaq.modules.render;

import org.lwjgl.glfw.GLFW;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.modules.settings.ColorSetting;
import com.xybakaqaq.utils.RenderUtils;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Highlights the block the crosshair is pointing at with a colored box (outline + optional
 * translucent fill), drawn through the vanilla gizmo system on top of the default thin hit
 * outline. Color / fill / through-walls are configurable.
 */
public final class BlockHighlight extends Module {

	private final ColorSetting color = new ColorSetting("Color", 0xFFC20C0C);
	private final BooleanSetting fill = new BooleanSetting("Fill", false);
	private final BooleanSetting throughWalls = new BooleanSetting("ThroughWalls", true);

	public BlockHighlight() {
		super("BlockHighlight", Category.RENDER, "Highlights the block you are looking at.", GLFW.GLFW_KEY_UNKNOWN);
	}

	@Override
	public void onRegister() {
		RenderUtils.suppressVanillaBlockOutlineWhen(this::isEnabled);
		RenderUtils.onFrame(() -> {
			if (!isEnabled() || mc.level == null
					|| !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
				return;
			}
			int argb = color.getColor();
			int stroke = 0xFF000000 | (argb & 0xFFFFFF);
			int fillArgb = fill.isEnabled() ? (0x40000000 | (argb & 0xFFFFFF)) : 0;
			RenderUtils.blockBox(hit.getBlockPos(), stroke, 2.0f, fillArgb, throughWalls.isEnabled());
		});
	}
}
