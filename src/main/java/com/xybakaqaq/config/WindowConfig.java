package com.xybakaqaq.config;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// Persists on-screen window positions to windows.json, keyed by a window name (e.g.
// "arraylist"). Loaded lazily on first access; #set writes through immediately.
public final class WindowConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static JsonObject root;

	private WindowConfig() {
	}

	public static int getX(String name, int fallback) {
		return read(name, "x", fallback);
	}

	public static int getY(String name, int fallback) {
		return read(name, "y", fallback);
	}

	// Stores and persists a window's position.
	public static void set(String name, int x, int y) {
		JsonObject root = root();
		JsonObject entry = root.has(name) ? root.getAsJsonObject(name) : new JsonObject();
		entry.addProperty("x", x);
		entry.addProperty("y", y);
		root.add(name, entry);
		save(root);
	}

	// Reads a fractional position component (0..1 along the free travel range, stored under
	// "fx"/"fy" by #setFraction). Falls back to fallback when absent
	// (e.g. only a legacy absolute "x"/"y" exists).
	public static double getFraction(String name, String key, double fallback) {
		JsonObject root = root();
		if (root.has(name)) {
			JsonObject entry = root.getAsJsonObject(name);
			if (entry.has(key)) {
				return entry.get(key).getAsDouble();
			}
		}
		return fallback;
	}

	// Stores and persists a window's fractional position (gui-scale independent).
	public static void setFraction(String name, double fx, double fy) {
		JsonObject root = root();
		JsonObject entry = root.has(name) ? root.getAsJsonObject(name) : new JsonObject();
		entry.addProperty("fx", fx);
		entry.addProperty("fy", fy);
		root.add(name, entry);
		save(root);
	}

	private static void save(JsonObject root) {
		try {
			Files.writeString(Config.windowsFile(), GSON.toJson(root));
		} catch (Exception ignored) {
			// best-effort
		}
	}

	private static int read(String name, String key, int fallback) {
		JsonObject root = root();
		if (root.has(name)) {
			JsonObject entry = root.getAsJsonObject(name);
			if (entry.has(key)) {
				return entry.get(key).getAsInt();
			}
		}
		return fallback;
	}

	private static JsonObject root() {
		if (root == null) {
			try {
				Path file = Config.windowsFile();
				root = Files.exists(file)
						? JsonParser.parseString(Files.readString(file)).getAsJsonObject()
						: new JsonObject();
			} catch (Exception ex) {
				root = new JsonObject();
			}
		}
		return root;
	}
}
