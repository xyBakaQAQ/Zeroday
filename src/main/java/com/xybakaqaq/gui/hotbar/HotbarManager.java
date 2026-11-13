package com.xybakaqaq.gui.hotbar;

import com.xybakaqaq.Zeroday;

import com.xybakaqaq.gui.hotbar.hotbars.ZerodayHotbar;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

// Manages the lifecycle of the client-side hotbar.
// Call #init() once during mod initialization.
public class HotbarManager {
	private static Hotbar hotbar;

	private HotbarManager() {
	}

	// Initializes the hotbar manager. Registered from the main mod class.
	public static void init() {
		hotbar = new ZerodayHotbar();
		Zeroday.LOGGER.info("HotbarManager initialized");
	}


	public static Hotbar getHotbar() {
		return hotbar;
	}

	// Routes the Hud hotbar render hook to the active Hotbar.
	public static boolean onExtractHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		return hotbar != null && hotbar.extract(graphics, deltaTracker);
	}

	// Routes the Hud selected-item-name hook to the active Hotbar.
	public static boolean onExtractSelectedItemName(GuiGraphicsExtractor graphics, ItemStack stack, int timer) {
		return hotbar != null && hotbar.extractSelectedItemName(graphics, stack, timer);
	}
}
