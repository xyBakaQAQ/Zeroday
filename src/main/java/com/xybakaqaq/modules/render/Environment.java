package com.xybakaqaq.modules.render;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.modules.settings.ModeSetting;
import com.xybakaqaq.modules.settings.NumberSetting;

import net.minecraft.world.level.biome.Biome;

/**
 * Client-only environment overrides in one module: local weather (Clear / Rain / Snow / Thunder, with
 * an adjustable intensity) and a locked time of day. Each is its own sub-toggle so they can be used
 * independently. Driven by {@link com.xybakaqaq.mixin.LevelMixin} + {@link
 * com.xybakaqaq.mixin.ClientLevelMixin} (weather) and {@link
 * com.xybakaqaq.mixin.ClientClockManagerMixin} (time), all guarded to the client level.
 */
public final class Environment extends Module {

	private static Environment instance;

	private final BooleanSetting weather = new BooleanSetting("Weather", true);
	private final ModeSetting weatherMode = new ModeSetting("Mode", "Rain", "Clear", "Rain", "Snow", "Thunder");
	private final NumberSetting intensity = new NumberSetting("Intensity", 1.0, 0.0, 1.0, 0.05);
	private final BooleanSetting time = new BooleanSetting("Time", false);
	private final NumberSetting timeOfDay = new NumberSetting("Time of Day", 6000, 0, 24000, 100);

	public Environment() {
		super("Environment", Category.RENDER, "Overrides local weather and time of day.", InputConstants.UNKNOWN.getValue());
		instance = this;
		weatherMode.visibleWhen(weather::isEnabled);
		intensity.visibleWhen(weather::isEnabled);
		timeOfDay.visibleWhen(time::isEnabled);
	}

	// ---------- weather ----------

	/** @return whether the weather override is active. */
	public static boolean weatherActive() {
		return instance != null && instance.isEnabled() && instance.weather.isEnabled();
	}

	/** @return the rain level to report for the client level (0 for Clear). */
	public static float rainLevel() {
		if (instance == null || instance.weatherMode.getValue().equals("Clear")) {
			return 0f;
		}
		return instance.intensity.getValue().floatValue();
	}

	/** @return the thunder level to report (intensity for Thunder, otherwise 0). */
	public static float thunderLevel() {
		return instance != null && instance.weatherMode.getValue().equals("Thunder")
				? instance.intensity.getValue().floatValue() : 0f;
	}

	/** @return the forced precipitation type, or {@code null} to leave it (Clear / inactive). */
	public static Biome.Precipitation precipType() {
		if (!weatherActive()) {
			return null;
		}
		return switch (instance.weatherMode.getValue()) {
			case "Snow" -> Biome.Precipitation.SNOW;
			case "Rain", "Thunder" -> Biome.Precipitation.RAIN;
			default -> null;
		};
	}

	// ---------- time ----------

	/** @return whether the time override is active. */
	public static boolean timeActive() {
		return instance != null && instance.isEnabled() && instance.time.isEnabled();
	}

	/** @return the overworld clock tick to report. */
	public static long ticks() {
		return instance == null ? 0L : instance.timeOfDay.getValue().longValue();
	}
}
