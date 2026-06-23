package com.xybakaqaq.event;

import com.xybakaqaq.event.features.KeyboardEvent;
import com.xybakaqaq.event.features.TickEvent;

/**
 * Implemented by anything that reacts to events (e.g. every {@code Module}). Override only the
 * hooks you need; the rest are no-ops.
 */
public interface EventListener {

	default void onTick(TickEvent event) {
	}

	default void onKey(KeyboardEvent event) {
	}
}
