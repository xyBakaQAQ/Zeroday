package com.xybakaqaq.config;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xybakaqaq.Zeroday;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.modules.settings.ColorSetting;
import com.xybakaqaq.modules.settings.ModeSetting;
import com.xybakaqaq.modules.settings.NumberSetting;
import com.xybakaqaq.modules.settings.Setting;
import com.xybakaqaq.utils.KeyUtils;

/**
 * Persists each module's on/off state, keybind and settings to {@code config.json}. {@link #load()}
 * applies the saved values to the registered modules, then wires auto-save: any later change (toggle,
 * rebind, setting change) calls {@link #save()} via {@link ModuleManager#notifyChanged()}.
 */
public final class ModuleConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private ModuleConfig() {
	}

	/** Loads saved state onto the (already registered) modules, then enables auto-save. */
	public static void load() {
		try {
			Path file = Config.configFile();
			if (Files.exists(file)) {
				JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
				// 26.2 stored GLFW keycodes; 26.3 reports SDL scancodes. A file without the marker
				// predates the switch, so its binds are converted on the way in.
				boolean legacy = !root.has("keyScheme")
						|| root.get("keyScheme").getAsInt() < KeyUtils.KEY_SCHEME_SDL;
				JsonObject modules = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
				for (Module module : ModuleManager.getModules()) {
					if (!modules.has(module.getName())) {
						continue;
					}
					JsonObject entry = modules.getAsJsonObject(module.getName());
					if (entry.has("key")) {
						int key = entry.get("key").getAsInt();
						module.setKey(legacy ? KeyUtils.glfwToScancode(key) : key);
					}
					if (entry.has("settings")) {
						JsonObject settings = entry.getAsJsonObject("settings");
						for (Setting<?> setting : module.getSettings()) {
							if (settings.has(setting.getName())) {
								applySetting(setting, settings.get(setting.getName()));
							}
						}
					}
					// Enable last, so onEnable() sees the final settings.
					if (entry.has("enabled")) {
						module.setEnabled(entry.get("enabled").getAsBoolean());
					}
				}
			}
		} catch (Exception ex) {
			Zeroday.LOGGER.warn("Failed to load config", ex);
		}
		ModuleManager.setChangeListener(ModuleConfig::save);
	}

	/** Writes the current state of every module to {@code config.json}. */
	public static void save() {
		try {
			JsonObject modules = new JsonObject();
			for (Module module : ModuleManager.getModules()) {
				JsonObject entry = new JsonObject();
				entry.addProperty("enabled", module.isEnabled());
				entry.addProperty("key", module.getKey());
				JsonObject settings = new JsonObject();
				for (Setting<?> setting : module.getSettings()) {
					Object value = setting.getValue();
					if (value instanceof Boolean bool) {
						settings.addProperty(setting.getName(), bool);
					} else if (value instanceof Number number) {
						settings.addProperty(setting.getName(), number);
					} else {
						settings.addProperty(setting.getName(), String.valueOf(value));
					}
				}
				entry.add("settings", settings);
				modules.add(module.getName(), entry);
			}
			JsonObject root = new JsonObject();
			root.addProperty("keyScheme", KeyUtils.KEY_SCHEME_SDL);
			root.add("modules", modules);
			Files.writeString(Config.configFile(), GSON.toJson(root));
		} catch (Exception ex) {
			Zeroday.LOGGER.warn("Failed to save config", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static void applySetting(Setting<?> setting, JsonElement value) {
		if (setting instanceof BooleanSetting bool) {
			bool.setValue(value.getAsBoolean());
		} else if (setting instanceof ModeSetting mode) {
			mode.setValue(value.getAsString());
		} else if (setting instanceof NumberSetting number) {
			number.setValue(value.getAsDouble());
		} else if (setting instanceof ColorSetting color) {
			color.setValue(value.getAsInt());
		} else {
			((Setting<Object>) setting).setValue(value.getAsString());
		}
	}
}
