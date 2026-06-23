package com.xybakaqaq.event;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;

import com.xybakaqaq.Zeroday;
import com.xybakaqaq.event.features.KeyboardEvent;
import com.xybakaqaq.event.features.TickEvent;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.ModuleManager;

/**
 * Posts events directly to every {@link Module} (each is an {@link EventListener}). Mixins call
 * the {@code onX} helpers; {@link #post(Event)} then visits each module via {@link Event#call}.
 *
 * <p>Keyboard events reach all modules (so a disabled module can still see its toggle key);
 * other events only reach enabled modules.
 */
public final class EventManager {

	private static final TickEvent TICK_EVENT = new TickEvent();

	private EventManager() {
	}

	/** Called once from the main mod class. */
	public static void init() {
		Zeroday.LOGGER.info("EventManager initialized");
	}

	public static void onTick() {
		post(TICK_EVENT);
	}

	public static void onKey(int key, int action) {
		if (key == GLFW_KEY_UNKNOWN) {
			return;
		}
		post(new KeyboardEvent(key, action));
	}

	public static void post(Event event) {
		boolean keyboardEvent = event instanceof KeyboardEvent;
		for (Module module : ModuleManager.getModules()) {
			if (keyboardEvent || module.isEnabled()) {
				event.call(module);
			}
		}
	}
}
