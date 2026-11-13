package com.xybakaqaq.config;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// Persists the client's UI preferences to ui.json (currently the custom menu switch).
public final class UiConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static JsonObject root;

	private UiConfig() {
	}

	// Whether the Skija menus (title screen, world select, multiplayer) are shown.
	public static boolean customMenu() {
		return read("customMenu", true);
	}

	public static void setCustomMenu(boolean value) {
		JsonObject root = root();
		root.addProperty("customMenu", value);
		save(root);
	}

	private static boolean read(String key, boolean fallback) {
		JsonObject root = root();
		return root.has(key) ? root.get(key).getAsBoolean() : fallback;
	}

	private static JsonObject root() {
		if (root == null) {
			try {
				Path file = Config.uiFile();
				root = Files.exists(file)
						? JsonParser.parseString(Files.readString(file)).getAsJsonObject()
						: new JsonObject();
			} catch (Exception ex) {
				root = new JsonObject();
			}
		}
		return root;
	}

	private static void save(JsonObject root) {
		try {
			Files.writeString(Config.uiFile(), GSON.toJson(root));
		} catch (Exception ignored) {
			// best-effort
		}
	}
}
