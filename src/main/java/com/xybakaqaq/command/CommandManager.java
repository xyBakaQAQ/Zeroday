package com.xybakaqaq.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.xybakaqaq.Zeroday;
import com.xybakaqaq.command.commands.HelpCommand;
import com.xybakaqaq.command.commands.ToggleCommand;
import com.xybakaqaq.modules.ModuleManager;

/**
 * Registry and dispatcher for chat commands. {@link #dispatch(String)} returns {@code true} for any
 * message it consumed, so the chat mixin can keep it off the server.
 *
 * <p>Commands only work while the {@code Command} module (client category) is enabled; that module
 * also owns the prefix.
 */
public final class CommandManager {

	/** Prefix used when the Command module has none configured. */
	public static final String DEFAULT_PREFIX = ".";

	private static final List<Command> COMMANDS = new ArrayList<>();

	private CommandManager() {
	}

	public static void init() {
		register(new ToggleCommand());
		register(new HelpCommand());
		Zeroday.LOGGER.info("CommandManager initialized ({} commands)", COMMANDS.size());
	}

	public static void register(Command command) {
		COMMANDS.add(command);
	}

	/** @return all registered commands (for {@code help}). */
	public static List<Command> commands() {
		return List.copyOf(COMMANDS);
	}

	/** @return whether chat commands are active (the Command module is on). */
	public static boolean enabled() {
		com.xybakaqaq.modules.client.Command module = module();
		return module != null && module.isEnabled();
	}

	/** @return the configured prefix, always non-empty. */
	public static String prefix() {
		com.xybakaqaq.modules.client.Command module = module();
		return module == null ? DEFAULT_PREFIX : module.getPrefix();
	}

	/** @return every command name and alias (for tab completion). */
	public static List<String> labels() {
		return COMMANDS.stream().flatMap(command -> command.labels().stream()).toList();
	}

	/** @return the command matching {@code label} (name or alias), or {@code null}. */
	public static Command get(String label) {
		return COMMANDS.stream().filter(command -> command.matches(label)).findFirst().orElse(null);
	}

	public static boolean dispatch(String message) {
		if (message == null || !enabled()) {
			return false;
		}
		String prefix = prefix();
		if (!message.startsWith(prefix)) {
			return false;
		}
		String[] parts = message.substring(prefix.length()).trim().split("\\s+");
		Command command = get(parts[0]);
		if (command != null) {
			command.execute(Arrays.copyOfRange(parts, 1, parts.length));
		}
		return true; // consume anything with the prefix so it never reaches the server
	}

	// Fully qualified: this package already has a `Command` (the base class).
	private static com.xybakaqaq.modules.client.Command module() {
		return ModuleManager.get(com.xybakaqaq.modules.client.Command.class);
	}
}
