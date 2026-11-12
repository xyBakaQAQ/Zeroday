package com.xybakaqaq.command;

import java.util.ArrayList;
import java.util.List;

import com.xybakaqaq.utils.IMinecraft;

import net.minecraft.network.chat.Component;

/**
 * Base class for a chat command: a name, optional aliases, a one-line description, and
 * {@link #execute(String[])}.
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

	/** @return the name followed by every alias (for tab completion). */
	public final List<String> labels() {
		List<String> labels = new ArrayList<>();
		labels.add(name);
		labels.addAll(List.of(aliases));
		return labels;
	}

	/** @return {@code true} if {@code label} is this command's name or one of its aliases. */
	public final boolean matches(String label) {
		if (name.equalsIgnoreCase(label)) {
			return true;
		}
		for (String alias : aliases) {
			if (alias.equalsIgnoreCase(label)) {
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
