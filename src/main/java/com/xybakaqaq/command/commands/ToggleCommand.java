package com.xybakaqaq.command.commands;

import java.util.List;

import com.xybakaqaq.command.Command;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.ModuleManager;

/**
 * {@code toggle <module>} — toggles a module by name.
 */
public final class ToggleCommand extends Command {

	public ToggleCommand() {
		super("toggle", "Toggle a module by name.", "t");
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 1) {
			info("Usage: " + getName() + " <module>");
			return;
		}
		Module module = ModuleManager.get(args[0]);
		if (module == null) {
			info("Unknown module: " + args[0]);
			return;
		}
		module.toggle();
		info(module.getName() + (module.isEnabled() ? " enabled" : " disabled"));
	}

	@Override
	public List<String> complete(String[] args) {
		if (args.length == 1) {
			return ModuleManager.getModules().stream().map(Module::getName).toList();
		}
		return List.of();
	}
}
