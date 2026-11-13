package com.xybakaqaq.modules.settings;

import java.util.function.BooleanSupplier;

import com.xybakaqaq.modules.ModuleManager;

// Base class for a module setting holding a single value of type T.
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

	// Makes this a child setting shown in the tree only while condition holds (e.g. while its
	// parent toggle is on). @return this, for chaining.
	public Setting<T> visibleWhen(BooleanSupplier condition) {
		this.visible = condition;
		this.child = true;
		return this;
	}


	public boolean isVisible() {
		return visible.getAsBoolean();
	}


	public boolean isChild() {
		return child;
	}
}
