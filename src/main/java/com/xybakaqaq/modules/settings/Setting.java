package com.xybakaqaq.modules.settings;

import com.xybakaqaq.modules.ModuleManager;

/**
 * Base class for a module setting holding a single value of type {@code T}.
 *
 * @param <T> the value type (e.g. {@link Boolean} for {@link BooleanSetting})
 */
public abstract class Setting<T> {

	private final String name;
	private T value;

	protected Setting(String name, T defaultValue) {
		this.name = name;
		this.value = defaultValue;
	}

	public String getName() {
		return name;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
		ModuleManager.notifyChanged();
	}
}
