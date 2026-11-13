package com.xybakaqaq.utils;

import java.util.Arrays;

import net.minecraft.client.DeltaTracker;

// Layout and animation math for the Zeroday-style hotbar.
// The hotbar is drawn as a magnified "dock": the selected slot is enlarged and nearby
// slots scale down smoothly with distance. A small amount of per-frame state is kept here
// so the magnification and the moving selection ease toward their targets instead of
// snapping. All smoothing is frame-rate independent (driven by the realtime tick delta).
public final class HotbarLayout {

	public static final int SLOT_COUNT = 9;

	// Scale applied to the focused slot; neighbours interpolate down toward 1.0.
	private static final float MAX_SCALE = 1.6f;
	// Slots farther than this (in slot units) from the focus stay at scale 1.0.
	private static final float INFLUENCE = 4f;

	// Horizontal distance between slot centres (wider than vanilla's 20 to fit enlarged icons).
	public static final int SLOT_PITCH = 24;

	// Easing speeds, per realtime tick. Higher = snappier.
	private static final float SCALE_SPEED = 0.6f;
	private static final float FOCUS_SPEED = 0.7f;

	// Per-frame animation state (single hotbar instance).
	private static final float[] scales = filled(SLOT_COUNT, 1f);
	private static float focus = 0f;

	private HotbarLayout() {
	}

	// Target magnification for slot given the (smoothed) focus position.
	public static float targetScale(float focusSlot, int slot) {
		float dist = Math.abs(focusSlot - slot);
		if (dist >= INFLUENCE) {
			return 1f;
		}
		float t = dist / INFLUENCE;        // 0 at focus .. 1 at edge of influence
		float falloff = 1f - t * t;        // smooth bell-shaped falloff
		return 1f + (MAX_SCALE - 1f) * falloff;
	}

	// Eases and returns the smoothed scale for slot.
	public static float animatedScale(float focusSlot, int slot, DeltaTracker delta) {
		float dt = delta.getRealtimeDeltaTicks();
		scales[slot] = approach(scales[slot], targetScale(focusSlot, slot), SCALE_SPEED, dt);
		return scales[slot];
	}

	// Eases and returns the smoothed focus (selected-slot) position.
	public static float animatedFocus(int selected, DeltaTracker delta) {
		float dt = delta.getRealtimeDeltaTicks();
		focus = approach(focus, selected, FOCUS_SPEED, dt);
		return focus;
	}

	// Screen-space centre X of a slot (slot 4 sits on the screen centre).
	public static int slotCenterX(int screenCenterX, int slot) {
		return screenCenterX + (slot - 4) * SLOT_PITCH;
	}

	// Screen-space centre X for a fractional slot position (lets the tooltip slide smoothly).
	public static int slotCenterX(int screenCenterX, float slot) {
		return Math.round(screenCenterX + (slot - 4) * SLOT_PITCH);
	}

	// The current smoothed focus (selected-slot) position, as updated this frame.
	public static float focus() {
		return focus;
	}

	// The maximum slot magnification, for callers that need the enlarged icon height.
	public static float maxScale() {
		return MAX_SCALE;
	}

	// Frame-rate independent ease toward target.
	private static float approach(float current, float target, float speed, float dt) {
		float factor = 1f - (float) Math.exp(-speed * dt);
		return current + (target - current) * factor;
	}

	private static float[] filled(int n, float value) {
		float[] a = new float[n];
		Arrays.fill(a, value);
		return a;
	}
}
