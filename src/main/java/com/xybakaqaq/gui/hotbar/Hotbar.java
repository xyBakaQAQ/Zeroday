package com.xybakaqaq.gui.hotbar;

import com.xybakaqaq.gui.hotbar.hotbars.ZerodayHotbar;
import com.xybakaqaq.utils.IMinecraft;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

// Client-side hotbar.
// #extract is the rendering hook driven by the Hud mixin. Returning
// true cancels vanilla hotbar rendering so this class can draw its own; returning
// false lets vanilla render as usual.
public class Hotbar implements IMinecraft {

	public Hotbar() {
	}

	// Called every frame from the Hud.extractItemHotbar mixin, before vanilla draws.
	public boolean extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		// Default: do not override the vanilla hotbar. Build custom rendering here and
		// return true to take over.
		return false;
	}

	// Called from the Hud.extractSelectedItemName mixin to draw the held-item name
	// (and any detailed stat line). Default is a no-op; ZerodayHotbar overrides it.
	public boolean extractSelectedItemName(GuiGraphicsExtractor graphics, ItemStack stack, int timer) {
		// Default: do not override the vanilla item name.
		return false;
	}
}
