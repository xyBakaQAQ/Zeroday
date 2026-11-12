package com.xybakaqaq.modules.render;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.settings.BooleanSetting;

/**
 * On-screen key display: the movement keys (W/A/S/D) and the mouse buttons, highlighting each while
 * pressed, with optional Space and Shift rows and a CPS (clicks-per-second) counter on the mouse
 * buttons. Rendered by {@link com.xybakaqaq.gui.keystrokes.KeystrokesWidget} (Skija) and draggable
 * like the other HUD widgets while the chat bar is open.
 */
public final class Keystrokes extends Module {

	private final BooleanSetting space = new BooleanSetting("Space", true);
	private final BooleanSetting shift = new BooleanSetting("Shift", true);
	private final BooleanSetting cps = new BooleanSetting("CPS", false);

	public Keystrokes() {
		super("Keystrokes", Category.RENDER, "Shows your movement + mouse keys on screen.", InputConstants.UNKNOWN.getValue());
	}

	public boolean showSpace() {
		return space.isEnabled();
	}

	public boolean showShift() {
		return shift.isEnabled();
	}

	public boolean showCps() {
		return cps.isEnabled();
	}
}
