package com.xybakaqaq.gui.ui;

/**
 * An animated {@code float} that eases toward a target each frame (frame-rate independent
 * exponential smoothing). Set a new target with {@link #set}, advance with {@link #update}, read the
 * eased value with {@link #value}. Used for hover/press/expand transitions on components.
 */
public final class Anim {

	private float value;
	private float target;
	private float speed = 14f;

	public Anim(float initial) {
		this.value = initial;
		this.target = initial;
	}

	/** Higher = snappier. Default 14. */
	public Anim speed(float speed) {
		this.speed = speed;
		return this;
	}

	public void set(float target) {
		this.target = target;
	}

	public void snap(float value) {
		this.value = value;
		this.target = value;
	}

	public float value() {
		return value;
	}

	public float target() {
		return target;
	}

	/** Advances by {@code dt} seconds toward the target. */
	public void update(float dt) {
		float t = 1f - (float) Math.exp(-speed * dt);
		value += (target - value) * t;
	}
}
