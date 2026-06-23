package com.xybakaqaq.gui.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A minimal observable value for MVVM binding: the ViewModel holds {@code State}s, views subscribe
 * and react to changes. {@link #set} notifies listeners only when the value actually changes.
 *
 * @param <T> the value type
 */
public final class State<T> {

	private T value;
	private final List<Consumer<T>> listeners = new ArrayList<>();

	public State(T initial) {
		this.value = initial;
	}

	public T get() {
		return value;
	}

	public void set(T next) {
		if (next == null ? value == null : next.equals(value)) {
			return;
		}
		value = next;
		for (Consumer<T> listener : List.copyOf(listeners)) {
			listener.accept(value);
		}
	}

	/** Subscribes to changes; immediately invokes {@code listener} with the current value. */
	public State<T> bind(Consumer<T> listener) {
		listeners.add(listener);
		listener.accept(value);
		return this;
	}
}
