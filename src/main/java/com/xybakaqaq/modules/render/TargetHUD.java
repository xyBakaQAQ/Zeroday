package com.xybakaqaq.modules.render;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.settings.NumberSetting;

// Skija-rendered target HUD: shows the most recently attacked entity (head for players, name + an
// animated health bar), and hides once it is further than the configured distance. With no target,
// it shows yourself while the chat bar is open. Draggable like the other HUD widgets.
public final class TargetHUD extends Module {

	private final NumberSetting distance = new NumberSetting("Distance", 6.0, 1.0, 10.0, 1.0);

	public TargetHUD() {
		super("TargetHUD", Category.RENDER, "Skija target HUD (drag while chat is open).", InputConstants.UNKNOWN.getValue());
	}


	public double distance() {
		return distance.getValue();
	}
}
