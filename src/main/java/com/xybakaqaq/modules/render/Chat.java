package com.xybakaqaq.modules.render;

import org.lwjgl.glfw.GLFW;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;

/**
 * Replaces the vanilla chat message list with the Skija-rendered {@code ChatWidget} (same lines,
 * same fade), draggable while the chat bar is open. The input box itself is still vanilla.
 */
public final class Chat extends Module {

	public Chat() {
		super("Chat", Category.RENDER, "Skija-rendered draggable chat log.", GLFW.GLFW_KEY_UNKNOWN);
	}
}
