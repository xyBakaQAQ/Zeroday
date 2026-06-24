package com.xybakaqaq.modules.settings;

import java.util.function.BooleanSupplier;

import com.xybakaqaq.modules.ModuleManager;

/**
 * Base class for a module setting holding a single value of type {@code T}.
 *
 * @param <T> the value type (e.g. {@link Boolean} for {@link BooleanSetting})
 */
public abstract class Setting<T> {

	private final String name;
	private T value;
	private BooleanSupplier visible = () -> true;
	private boolean child;

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

	/**
	 * Makes this a child setting shown in the tree only while {@code condition} holds (e.g. while its
	 * parent toggle is on). @return this, for chaining.
	 */
	public Setting<T> visibleWhen(BooleanSupplier condition) {
		this.visible = condition;
		this.child = true;
		return this;
	}

	/** @return whether this setting should currently appear in the settings tree. */
	public boolean isVisible() {
		return visible.getAsBoolean();
	}

	/** @return whether this is a child (sub-) setting, rendered indented under its parent. */
	public boolean isChild() {
		return child;
	}
}
