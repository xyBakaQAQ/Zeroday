package com.xybakaqaq.modules.client;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.gui.clickgui.ClickGuiScreen;
import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;

// Toggles the Skija-rendered ClickGUI (default Right Shift). Acts as a trigger: the key opens the
// screen if it isn't showing, or closes it if it is.
public final class ClickGui extends Module {

	public ClickGui() {
		super("ClickGui", Category.CLIENT, "Opens the Skija click GUI.", InputConstants.KEY_RSHIFT);
	}

	@Override
	protected void onEnable() {
		mc.execute(() -> {
			if (mc.gui.screen() instanceof ClickGuiScreen) {
				mc.gui.setScreen(null);
			} else {
				mc.gui.setScreen(new ClickGuiScreen());
			}
		});
		setEnabled(false);
	}
}
