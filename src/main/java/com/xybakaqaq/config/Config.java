package com.xybakaqaq.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Central definition of the mod's on-disk layout, under {@code .minecraft/zeroday/}:
 *
 * <pre>
 *   config.json            module on/off, keybinds and settings
 *   windows.json           on-screen window positions (e.g. the music widget xy)
 *   cloudmusic/
 *     cookie               NetEase login token
 *     cache/               downloaded cover thumbnails
 * </pre>
 */
public final class Config {

	private static final Path DIR = FabricLoader.getInstance().getGameDir().resolve("zeroday");

	private Config() {
	}

	public static Path dir() {
		return ensure(DIR);
	}

	public static Path configFile() {
		return dir().resolve("config.json");
	}

	public static Path windowsFile() {
		return dir().resolve("windows.json");
	}

	public static Path cloudmusicDir() {
		return ensure(DIR.resolve("cloudmusic"));
	}

	public static Path cookieFile() {
		return cloudmusicDir().resolve("cookie");
	}

	public static Path coverCacheDir() {
		return ensure(cloudmusicDir().resolve("cache"));
	}

	private static Path ensure(Path path) {
		try {
			Files.createDirectories(path);
		} catch (IOException ignored) {
			// best-effort
		}
		return path;
	}
}
