package com.xybakaqaq.modules.settings;

// A setting holding an ARGB color (0xAARRGGBB). The ClickGUI renders it as a swatch that expands
// into a saturation/value square + hue strip picker.
public final class ColorSetting extends Setting<Integer> {

	public ColorSetting(String name, int argb) {
		super(name, argb);
	}

	public int getColor() {
		return getValue();
	}

	public void setColor(int argb) {
		setValue(argb);
	}
}
