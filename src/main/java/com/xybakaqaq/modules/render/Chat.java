package com.xybakaqaq.modules.render;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.modules.settings.ColorSetting;
import com.xybakaqaq.modules.settings.NumberSetting;

// Replaces the vanilla chat message list with the Skija-rendered ChatWidget (same lines,
// same fade), draggable while the chat bar is open. The input box itself is still vanilla.
public final class Chat extends Module {

	private final BooleanSetting adaptiveWidth = new BooleanSetting("Adaptive Width", false);
	private final BooleanSetting antiSpam = new BooleanSetting("Anti Spam", true);
	private final NumberSetting maxLines = new NumberSetting("Max Lines", 100, 20, 1000, 20);
	private final BooleanSetting keepHistory = new BooleanSetting("Keep History", false);
	private final ColorSetting background = new ColorSetting("Background", 0xB0000000);

	public Chat() {
		super("Chat", Category.RENDER, "Skija-rendered draggable chat log.", InputConstants.UNKNOWN.getValue());
	}

	public boolean adaptiveWidth() {
		return adaptiveWidth.isEnabled();
	}


	public boolean antiSpam() {
		return antiSpam.isEnabled();
	}


	public int maxLines() {
		return (int) Math.round(maxLines.getValue());
	}


	public boolean keepHistory() {
		return keepHistory.isEnabled();
	}

	public int background() {
		return background.getColor();
	}
}
