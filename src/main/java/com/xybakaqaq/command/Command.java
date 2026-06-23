package com.xybakaqaq.command;

import java.util.List;

import com.xybakaqaq.utils.IMinecraft;

import net.minecraft.network.chat.Component;

/**
 * Base class for a chat command. A command has a name, a description and optional aliases;
 * {@link #execute(String[])} runs it with the arguments after the command name.
 */
public abstract class Command implements IMinecraft {

	private final String name;
	private final String description;
	private final String[] aliases;

	protected Command(String name, String description, String... aliases) {
		this.name = name;
		this.description = description;
		this.aliases = aliases;
	}

	public final String getName() {
		return name;
	}

	public final String getDescription() {
		return description;
	}

	public final String[] getAliases() {
		return aliases;
	}

	/** @return {@code true} if {@code label} is this command's name or one of its aliases. */
	public final boolean matches(String label) {
		if (label.equalsIgnoreCase(name)) {
			return true;
		}
		for (String alias : aliases) {
			if (label.equalsIgnoreCase(alias)) {
				return true;
			}
		}
		return false;
	}

	/** Runs the command. {@code args} is everything after the command name. */
	public abstract void execute(String[] args);

	/**
	 * Tab-completion candidates for the current (last) argument. {@code args} is the arguments
	 * typed so far, the last being the partial word being completed. Default: no suggestions.
	 */
	public List<String> complete(String[] args) {
		return List.of();
	}

	/** Prints a client-only chat message (command feedback). */
	protected final void info(String message) {
		if (mc.gui != null) {
			mc.gui.hud.getChat().addClientSystemMessage(Component.literal(message));
		}
	}
}
