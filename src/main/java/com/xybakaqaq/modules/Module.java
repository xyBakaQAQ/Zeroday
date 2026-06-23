package com.xybakaqaq.modules;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.xybakaqaq.event.EventListener;
import com.xybakaqaq.event.features.KeyboardEvent;
import com.xybakaqaq.modules.settings.Setting;
import com.xybakaqaq.utils.IMinecraft;

/**
 * Base class for a feature module. Every module is an {@link EventListener}: override the event
 * hooks ({@link #onTick}, ...) you need. The default {@link #onKey} toggles the module on its
 * bound key. {@code mc} comes from {@link IMinecraft}.
 */
public abstract class Module implements EventListener, IMinecraft {

	private final String name;
	private final Category category;
	private final String description;
	/** GLFW key code bound to this module ({@link GLFW#GLFW_KEY_UNKNOWN} = unbound). */
	private int key;
	private boolean enabled;
	/** Lazily collected from the subclass's {@link Setting} fields (no manual registration). */
	private List<Setting<?>> settings;

	protected Module(String name, Category category, String description) {
		this(name, category, description, GLFW.GLFW_KEY_UNKNOWN, false);
	}

	protected Module(String name, Category category, String description, int key) {
		this(name, category, description, key, false);
	}

	protected Module(String name, Category category, String description, int key, boolean defaultEnabled) {
		this.name = name;
		this.category = category;
		this.description = description;
		this.key = key;
		this.enabled = defaultEnabled;
	}

	public String getName() {
		return name;
	}

	public Category getCategory() {
		return category;
	}

	public String getDescription() {
		return description;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/** @return the bound GLFW key code, or {@link GLFW#GLFW_KEY_UNKNOWN} if unbound. */
	public int getKey() {
		return key;
	}

	/** Binds (or unbinds, with {@link GLFW#GLFW_KEY_UNKNOWN}) the toggle key. */
	public void setKey(int key) {
		this.key = key;
		ModuleManager.notifyChanged();
	}

	/**
	 * @return this module's settings, auto-discovered (once, then cached) by reflecting over the
	 *         {@link Setting} fields declared on the subclass — no manual registration needed.
	 */
	public List<Setting<?>> getSettings() {
		if (settings == null) {
			settings = collectSettings();
		}
		return settings;
	}

	private List<Setting<?>> collectSettings() {
		List<Setting<?>> collected = new ArrayList<>();
		for (Class<?> type = getClass(); type != null && type != Module.class; type = type.getSuperclass()) {
			for (Field field : type.getDeclaredFields()) {
				if (Setting.class.isAssignableFrom(field.getType())) {
					field.setAccessible(true);
					try {
						if (field.get(this) instanceof Setting<?> setting) {
							collected.add(setting);
						}
					} catch (IllegalAccessException ignored) {
						// inaccessible field — skip it
					}
				}
			}
		}
		return Collections.unmodifiableList(collected);
	}

	/** Enables/disables the module, firing {@link #onEnable()} or {@link #onDisable()} on change. */
	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
		ModuleManager.notifyChanged();
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	/** Called once when the module is registered. */
	public void onRegister() {
	}

	@Override
	public void onKey(KeyboardEvent event) {
		if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == key) {
			toggle();
		}
	}

	/** Called when the module becomes enabled. */
	protected void onEnable() {
	}

	/** Called when the module becomes disabled. */
	protected void onDisable() {
	}
}
