package com.xybakaqaq.modules.settings;

import java.util.List;

// A setting that cycles through a fixed list of named modes.
public class ModeSetting extends Setting<String> {

	private final List<String> modes;

	public ModeSetting(String name, String defaultMode, String... modes) {
		super(name, defaultMode);
		this.modes = List.of(modes);
	}

	public List<String> getModes() {
		return modes;
	}


	public boolean is(String mode) {
		return getValue().equals(mode);
	}

	// Advances to the next mode, wrapping around to the first.
	public void cycle() {
		int index = modes.indexOf(getValue());
		setValue(modes.get((index + 1) % modes.size()));
	}
}
