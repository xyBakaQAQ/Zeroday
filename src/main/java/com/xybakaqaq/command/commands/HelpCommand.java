package com.xybakaqaq.command.commands;

import java.util.stream.Collectors;

import com.xybakaqaq.command.Command;
import com.xybakaqaq.command.CommandManager;

// help — lists every registered command.
public final class HelpCommand extends Command {

	public HelpCommand() {
		super("help", "List available commands.", "h", "?");
	}

	@Override
	public void execute(String[] args) {
		String prefix = CommandManager.prefix();
		for (Command command : CommandManager.commands()) {
			String aliases = command.labels().stream().skip(1).collect(Collectors.joining(", "));
			info(prefix + command.getName()
					+ (aliases.isEmpty() ? "" : " (" + aliases + ")")
					+ " - " + command.getDescription());
		}
	}
}
