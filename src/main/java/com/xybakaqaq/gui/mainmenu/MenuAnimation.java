package com.xybakaqaq.gui.mainmenu;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

// Frame clock, smoothed hover and the page open animation. Every menu page shares the same animated
// backdrop, so switching between them is continuous: content slides up into place instead of the
// whole screen flashing.
public final class MenuAnimation {

	private static final float OPEN_SECONDS = 0.22f;

	private static final Map<AbstractWidget, Float> HOVER = new IdentityHashMap<>();

	private static Screen screen;
	private static long lastNanos;
	private static long openNanos;
	private static float dt;

	private MenuAnimation() {
	}

	// Called once per frame, before anything else is drawn (ScreenMixin).
	public static void begin(Screen current) {
		long now = System.nanoTime();
		dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
		lastNanos = now;
		if (current != screen) {
			screen = current;
			HOVER.clear();
			openNanos = now;
		}
	}

	// Eased 0..1 over the first frames of a page: 0 right after it opens, 1 once settled.
	public static float open() {
		if (openNanos == 0L) {
			return 1f;
		}
		float t = Math.min(1f, (System.nanoTime() - openNanos) / 1.0e9f / OPEN_SECONDS);
		float remain = 1f - t;
		return 1f - remain * remain * remain;
	}

	// Lerps 0..1 towards the hovered state.
	public static float hover(AbstractWidget widget, boolean hovered) {
		float value = HOVER.getOrDefault(widget, 0f);
		value += ((hovered ? 1f : 0f) - value) * Math.min(1f, dt * 14f);
		HOVER.put(widget, value);
		return value;
	}
}
