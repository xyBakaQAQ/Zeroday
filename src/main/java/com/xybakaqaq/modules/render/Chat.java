package com.xybakaqaq.modules.render;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.modules.settings.ColorSetting;

/**
 * Replaces the vanilla chat message list with the Skija-rendered {@code ChatWidget} (same lines,
 * same fade), draggable while the chat bar is open. The input box itself is still vanilla.
 */
public final class Chat extends Module {

	private final BooleanSetting adaptiveWidth = new BooleanSetting("Adaptive Width", false);
	private final ColorSetting background = new ColorSetting("Background", 0xB0000000);

	public Chat() {
		super("Chat", Category.RENDER, "Skija-rendered draggable chat log.", InputConstants.UNKNOWN.getValue());
	}

	/** @return whether the panel narrows to the longest line (otherwise it keeps a fixed width). */
	public boolean adaptiveWidth() {
		return adaptiveWidth.isEnabled();
	}

	/** @return the per-row backdrop color (ARGB). */
	public int background() {
		return background.getColor();
	}
}
