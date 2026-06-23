package com.xybakaqaq.gui.cloudmusic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.platform.NativeImage;
import com.xybakaqaq.config.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Lazily-loaded, per-song cover thumbnail cache for the track list. {@link #get} returns the
 * texture id once it is ready (or {@code null} meanwhile), kicking off a one-shot async load the
 * first time a song id is requested. Loads happen only for rows that actually get drawn.
 */
public final class CoverCache {

	public static final int PX = 32;

	private static final Map<Long, Identifier> ready = new ConcurrentHashMap<>();
	private static final Set<Long> loading = ConcurrentHashMap.newKeySet();

	private CoverCache() {
	}

	/** @return the cover texture id for {@code id} if loaded, else {@code null} (load kicked off once). */
	public static Identifier get(long id, String url) {
		Identifier got = ready.get(id);
		if (got != null) {
			return got;
		}
		if (url != null && !url.isEmpty() && loading.add(id)) {
			Thread thread = new Thread(() -> load(id, url), "cm-cover-" + id);
			thread.setDaemon(true);
			thread.start();
		}
		return null;
	}

	private static void load(long id, String url) {
		try {
			byte[] bytes = fetchBytes(id, url, PX);
			NativeImage image = Images.decode(bytes);
			Identifier texId = Identifier.fromNamespaceAndPath("zeroday", "cm_cover_" + id);
			Minecraft.getInstance().execute(() -> {
				Minecraft.getInstance().getTextureManager().register(texId, new DynamicTexture(() -> "cm-cover", image));
				ready.put(id, texId);
			});
		} catch (Exception ex) {
			loading.remove(id); // allow a later retry
		}
	}

	/**
	 * Fetches cover bytes for a song at the given pixel size, caching them on disk under
	 * {@code cloudmusic/cache/}. Subsequent sessions read from disk instead of the network.
	 */
	public static byte[] fetchBytes(long id, String url, int px) throws Exception {
		Path file = Config.coverCacheDir().resolve(id + "_" + px + ".jpg");
		if (Files.exists(file)) {
			return Files.readAllBytes(file);
		}
		byte[] bytes = NeteaseApi.httpBytes(url + "?param=" + px + "y" + px);
		try {
			Files.write(file, bytes);
		} catch (Exception ignored) {
			// disk cache is best-effort
		}
		return bytes;
	}
}
