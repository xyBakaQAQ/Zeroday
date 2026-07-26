package com.xybakaqaq.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xybakaqaq.Zeroday;
import com.xybakaqaq.modules.movement.Sprint;
import com.xybakaqaq.modules.render.Animation;
import com.xybakaqaq.modules.client.ClickGui;
import com.xybakaqaq.modules.client.Target;
import com.xybakaqaq.modules.render.BlockHighlight;
import com.xybakaqaq.modules.render.Chat;
import com.xybakaqaq.modules.render.Crosshair;
import com.xybakaqaq.modules.render.Environment;
import com.xybakaqaq.modules.render.FullBright;
import com.xybakaqaq.modules.render.HUD;
import com.xybakaqaq.modules.player.InventoryHelper;
import com.xybakaqaq.modules.render.Keystrokes;
import com.xybakaqaq.modules.render.TargetHUD;

/**
 * Registry for all {@link Module}s.
 *
 * <p>{@link #init()} is called once from the main mod class; register modules there via
 * {@link #register(Module)}. Event delivery is handled by {@code EventManager}, which posts
 * directly to each module's {@link Module#onEvent}.
 */
public final class ModuleManager {

	private static final List<Module> MODULES = new ArrayList<>();

	/** Notified whenever a module's state changes (toggle / rebind / setting). Wired to config save. */
	private static Runnable changeListener = () -> {
	};

	private ModuleManager() {
	}

	/** Registers a listener invoked on any module-state change (used for config auto-save). */
	public static void setChangeListener(Runnable listener) {
		changeListener = listener;
	}

	/** Fires the change listener (no-op until one is registered). */
	public static void notifyChanged() {
		changeListener.run();
	}

	/** Registers the built-in modules. Called once during mod init. */
	public static void init() {
		register(new HUD());
		register(new Sprint());
		register(new FullBright());
		register(new ClickGui());
		register(new Animation());
		register(new TargetHUD());
		register(new InventoryHelper());
		register(new Target());
		register(new Crosshair());
		register(new Environment());
		register(new Keystrokes());
		register(new BlockHighlight());
		register(new Chat());
		Zeroday.LOGGER.info("ModuleManager initialized ({} modules)", MODULES.size());
	}

	/** Adds a module to the registry and fires its {@link Module#onRegister()} hook. */
	public static void register(Module module) {
		MODULES.add(module);
		module.onRegister();
	}

	/** @return an unmodifiable view of all registered modules. */
	public static List<Module> getModules() {
		return Collections.unmodifiableList(MODULES);
	}

	/** @return the module with the given name (case-insensitive), or {@code null} if none. */
	public static Module get(String name) {
		for (Module module : MODULES) {
			if (module.getName().equalsIgnoreCase(name)) {
				return module;
			}
		}
		return null;
	}

	/** @return the registered module of the given type, or {@code null} if none. */
	public static <T extends Module> T get(Class<T> type) {
		for (Module module : MODULES) {
			if (type.isInstance(module)) {
				return type.cast(module);
			}
		}
		return null;
	}
}
