package com.xybakaqaq.utils;

import java.util.concurrent.CompletableFuture;

import net.minecraft.client.Minecraft;

// Vanilla answers a language change with a full resource reload: the pack repository is re-discovered
// from disk and every texture, model, sound and font is rebuilt, which is why the switch stalls behind
// the "Loading resource packs..." overlay for seconds even though nothing but the language files changed.
// Only the language files depend on the selected language, so this refreshes just those and leaves the
// rest of the loaded resources untouched.
public final class LanguageReloader {

	private LanguageReloader() {
	}

	// Re-reads the .json language files of the selected language and re-injects them into the global
	// Language, which is exactly what the reload listener does during a reload. Runs on the calling
	// thread (a handful of small reads) and returns a completed future so callers expecting the reload
	// future keep working unchanged.
	public static CompletableFuture<Void> reloadLanguageOnly(Minecraft minecraft) {
		minecraft.getLanguageManager().onResourceManagerReload(minecraft.getResourceManager());
		return CompletableFuture.completedFuture(null);
	}
}
