package com.xybakaqaq.event;

/**
 * Base event. Each concrete event implements {@link #call} to invoke the matching
 * {@link EventListener} method (visitor pattern), so dispatch needs no type checks.
 */
public abstract class Event {

	public abstract void call(EventListener listener);
}
