package com.xybakaqaq.modules.client;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.command.CommandManager;
import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.settings.StringSetting;

/**
 * Gates chat commands: while off, prefixed messages go to the server as normal chat; while on,
 * {@link CommandManager} intercepts them. The prefix is read live, so edits apply immediately.
 */
public final class Command extends Module {

	private final StringSetting prefix = new StringSetting("Prefix", CommandManager.DEFAULT_PREFIX, 3);

	public Command() {
		super("Command", Category.CLIENT, "Chat commands with an editable prefix.",
				InputConstants.UNKNOWN.getValue(), true);
	}

	/** @return the configured prefix, or the default if it was cleared. */
	public String getPrefix() {
		String value = prefix.getValue().trim();
		return value.isEmpty() ? CommandManager.DEFAULT_PREFIX : value;
	}
}
