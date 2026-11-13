package com.xybakaqaq.event.features;

import com.xybakaqaq.event.Event;
import com.xybakaqaq.event.EventListener;

// Posted for a raw GLFW key event (key = GLFW key code, action = GLFW action).
public final class KeyboardEvent extends Event {

	private final int key;
	private final int action;

	public KeyboardEvent(int key, int action) {
		this.key = key;
		this.action = action;
	}

	public int getKey() {
		return key;
	}

	public int getAction() {
		return action;
	}

	@Override
	public void call(EventListener listener) {
		listener.onKey(this);
	}
}
