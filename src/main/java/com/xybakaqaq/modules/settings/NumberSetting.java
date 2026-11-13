package com.xybakaqaq.modules.settings;

// A numeric setting bounded to [min, max] and snapped to step. Rendered as a slider
// in the ClickGUI.
public class NumberSetting extends Setting<Double> {

	private final double min;
	private final double max;
	private final double step;

	public NumberSetting(String name, double defaultValue, double min, double max, double step) {
		super(name, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getStep() {
		return step;
	}

	// Clamps to [min, max], snaps to the nearest step, then stores.
	public void setNumber(double value) {
		double clamped = Math.max(min, Math.min(max, value));
		double snapped = min + Math.round((clamped - min) / step) * step;
		setValue(Math.max(min, Math.min(max, snapped)));
	}
}
