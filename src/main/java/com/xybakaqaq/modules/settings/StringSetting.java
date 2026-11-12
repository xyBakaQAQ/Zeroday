package com.xybakaqaq.modules.settings;

/**
 * A free-form text setting, edited in the ClickGUI by clicking the row and typing.
 */
public class StringSetting extends Setting<String> {

	private final int maxLength;

	public StringSetting(String name, String defaultValue) {
		this(name, defaultValue, 32);
	}

	public StringSetting(String name, String defaultValue, int maxLength) {
		super(name, defaultValue);
		this.maxLength = maxLength;
	}

	public int getMaxLength() {
		return maxLength;
	}

	@Override
	public void setValue(String value) {
		super.setValue(value == null ? "" : value);
	}
}
