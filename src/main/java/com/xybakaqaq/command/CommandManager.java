package com.xybakaqaq.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.xybakaqaq.Zeroday;
import com.xybakaqaq.command.commands.ToggleCommand;

/**
 * Registry and dispatcher for chat commands. A chat message starting with {@link #PREFIX} is
 * routed here via {@link #dispatch(String)} (wire it from a chat-send mixin).
 */
public final class CommandManager {

	public static final String PREFIX = ".";

	private static final List<Command> COMMANDS = new ArrayList<>();

	private CommandManager() {
	}

	/** Registers the built-in commands. Called once from the main mod class. */
	public static void init() {
		register(new ToggleCommand());
		Zeroday.LOGGER.info("CommandManager initialized ({} commands)", COMMANDS.size());
	}

	public static void register(Command command) {
		COMMANDS.add(command);
	}

	public static List<Command> getCommands() {
		return Collections.unmodifiableList(COMMANDS);
	}

	/** @return all command names plus aliases (for tab completion). */
	public static List<String> getNames() {
		List<String> names = new ArrayList<>();
		for (Command command : COMMANDS) {
			names.add(command.getName());
			Collections.addAll(names, command.getAliases());
		}
		return names;
	}

	/** @return the command matching {@code label} (name or alias), or {@code null}. */
	public static Command get(String label) {
		for (Command command : COMMANDS) {
			if (command.matches(label)) {
				return command;
			}
		}
		return null;
	}

	/**
	 * If {@code message} starts with {@link #PREFIX}, runs the matching command and returns
	 * {@code true} so the caller can stop the message from being sent to the server.
	 */
	public static boolean dispatch(String message) {
		if (message == null || !message.startsWith(PREFIX)) {
			return false;
		}
		String[] parts = message.substring(PREFIX.length()).trim().split("\\s+");
		if (parts.length == 0 || parts[0].isEmpty()) {
			return false;
		}
		Command command = get(parts[0]);
		if (command != null) {
			command.execute(Arrays.copyOfRange(parts, 1, parts.length));
		}
		return true; // consume anything with the prefix (don't leak to chat)
	}
}
