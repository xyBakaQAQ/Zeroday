package com.xybakaqaq.utils;

import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.HUD;

// Shared HUD layout constants for the Zeroday hotbar.
// The Zeroday-style hotbar floats above the health bar, leaving the bottom strip (where
// the vanilla hotbar used to sit) empty. To fill it, the vanilla status bars — health,
// armor, food, air, vehicle health and the contextual (experience/jump) bar — are nudged
// downward by #STATUS_OFFSET pixels. The mixins do this by feeding the screen
// height through #shiftDown(int) wherever a status bar anchors to the bottom.
// The shift is only applied while the Zeroday hotbar is actually active; with the Hotbar
// set to off the vanilla hotbar and the status bars stay in their normal positions.
public final class HudLayout {

	// Downward shift, in scaled-GUI pixels, applied to the vanilla status bars.
	public static final int STATUS_OFFSET = 17;

	private HudLayout() {
	}

	// Returns a bottom-anchored screen height pushed down by the active hotbar offset.
	public static int shiftDown(int height) {
		return height + offset();
	}


	private static int offset() {
		HUD hud = ModuleManager.get(HUD.class);
		return hud == null || hud.isHotbarZeroday() ? STATUS_OFFSET : 0;
	}
}
