package com.xybakaqaq.mixin;

import com.mojang.blaze3d.platform.Window;

import com.xybakaqaq.utils.HudLayout;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Nudges the contextual bar (experience bar / jump bar / locator bar) and the experience
 * level number down by {@link HudLayout#STATUS_OFFSET}, matching the status-bar shift in
 * {@link HudMixin} so everything lines up at the bottom under the floating hotbar.
 */
@Mixin(ContextualBar.class)
public interface ContextualBarMixin {

	@Redirect(
			method = "top",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I")
	)
	private int zeroday$shiftBarDown(Window window) {
		return HudLayout.shiftDown(window.getGuiScaledHeight());
	}

	@Redirect(
			method = "extractExperienceLevel",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;guiHeight()I")
	)
	private static int zeroday$shiftLevelDown(GuiGraphicsExtractor graphics) {
		return HudLayout.shiftDown(graphics.guiHeight());
	}
}
