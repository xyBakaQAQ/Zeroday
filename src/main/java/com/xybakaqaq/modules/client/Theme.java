package com.xybakaqaq.modules.client;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;

// Gates the Skija skin for the screens you reach while playing: the ESC menu, everything opened from
// it (options, packs, social) and confirmations. It is a plain on/off switch in the ClickGUI; the
// pages shown outside a world follow the custom-menu switch on the title screen instead.
public final class Theme extends Module {

	public Theme() {
		super("Theme", Category.CLIENT, "Skins the in-game screens with the Skija theme.",
				InputConstants.UNKNOWN.getValue(), true);
	}
}
