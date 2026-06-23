package com.xybakaqaq.gui.hotbar;

import com.xybakaqaq.gui.hotbar.hotbars.ZerodayHotbar;
import com.xybakaqaq.utils.IMinecraft;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side hotbar.
 *
 * <p>{@link #extract} is the rendering hook driven by the {@code Hud} mixin. Returning
 * {@code true} cancels vanilla hotbar rendering so this class can draw its own; returning
 * {@code false} lets vanilla render as usual.
 */
public class Hotbar implements IMinecraft {

	public Hotbar() {
	}

	/**
	 * Called every frame from the {@code Hud.extractItemHotbar} mixin, before vanilla draws.
	 *
	 * @param graphics     the HUD render-state extractor (provides {@code guiWidth/guiHeight}, {@code blitSprite}, ...)
	 * @param deltaTracker the frame delta tracker
	 * @return {@code true} to replace (cancel) the vanilla hotbar, {@code false} to keep it
	 */
	public boolean extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		// Default: do not override the vanilla hotbar. Build custom rendering here and
		// return true to take over.
		return false;
	}

	/**
	 * Called from the {@code Hud.extractSelectedItemName} mixin to draw the held-item name
	 * (and any detailed stat line). Default is a no-op; {@link ZerodayHotbar} overrides it.
	 *
	 * @param graphics the HUD render-state extractor
	 * @param stack    the last highlighted held item
	 * @param timer    the held-item highlight fade timer (0 = nothing to show)
	 * @return {@code true} to replace (cancel) the vanilla item name, {@code false} to keep it
	 */
	public boolean extractSelectedItemName(GuiGraphicsExtractor graphics, ItemStack stack, int timer) {
		// Default: do not override the vanilla item name.
		return false;
	}
}
