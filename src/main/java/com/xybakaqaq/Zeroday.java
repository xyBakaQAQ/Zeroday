package com.xybakaqaq;

import com.xybakaqaq.command.CommandManager;
import com.xybakaqaq.config.ModuleConfig;
import com.xybakaqaq.event.EventManager;
import com.xybakaqaq.gui.hotbar.HotbarManager;
import com.xybakaqaq.gui.mainmenu.MainMenu;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.utils.RenderUtils;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Zeroday implements ModInitializer {
	public static final String MOD_ID = "zeroday";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		HotbarManager.init();
		EventManager.init();
		RenderUtils.init();
		ModuleManager.init();
		ModuleConfig.load();
		CommandManager.init();
		MainMenu.init();
	}
}