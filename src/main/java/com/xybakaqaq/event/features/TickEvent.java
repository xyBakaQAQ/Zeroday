package com.xybakaqaq.event.features;

import com.xybakaqaq.event.Event;
import com.xybakaqaq.event.EventListener;

// Posted once per client tick.
public final class TickEvent extends Event {

	@Override
	public void call(EventListener listener) {
		listener.onTick(this);
	}
}
