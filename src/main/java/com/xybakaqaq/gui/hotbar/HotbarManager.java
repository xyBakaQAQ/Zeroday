package com.xybakaqaq.gui.hotbar;

import com.xybakaqaq.Zeroday;

import com.xybakaqaq.gui.hotbar.hotbars.HanabiHotbar;
import com.xybakaqaq.gui.hotbar.hotbars.ZerodayHotbar;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.HUD;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

// Manages the lifecycle of the client-side hotbars.
// Call #init() once during mod initialization.
public class HotbarManager {
	private static Hotbar zerodayHotbar;
	private static Hotbar hanabiHotbar;

	private HotbarManager() {
	}

	// Initializes the hotbar manager. Registered from the main mod class.
	public static void init() {
		zerodayHotbar = new ZerodayHotbar();
		hanabiHotbar = new HanabiHotbar();
		Zeroday.LOGGER.info("HotbarManager initialized");
	}

	// The hotbar matching the HUD module's mode; the zeroday one is the fallback that also handles
	// the "off" mode by declining to render (so vanilla draws).
	public static Hotbar getHotbar() {
		HUD hud = ModuleManager.get(HUD.class);
		return hud != null && hud.isHotbarHanabi() ? hanabiHotbar : zerodayHotbar;
	}

	// Routes the Hud hotbar render hook to the active Hotbar.
	public static boolean onExtractHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Hotbar hotbar = getHotbar();
		return hotbar != null && hotbar.extract(graphics, deltaTracker);
	}

	// Routes the Hud selected-item-name hook to the active Hotbar.
	public static boolean onExtractSelectedItemName(GuiGraphicsExtractor graphics, ItemStack stack, int timer) {
		Hotbar hotbar = getHotbar();
		return hotbar != null && hotbar.extractSelectedItemName(graphics, stack, timer);
	}
}
