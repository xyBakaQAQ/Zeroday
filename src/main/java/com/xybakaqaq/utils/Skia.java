package com.xybakaqaq.utils;

import java.io.InputStream;

import io.github.humbleui.skija.Data;
import io.github.humbleui.skija.FontMgr;
import io.github.humbleui.skija.FontStyle;
import io.github.humbleui.skija.Typeface;

/**
 * Shared Skija typefaces. The bundled HarmonyOS Sans fonts (under {@code assets/zeroday/fonts/}) are
 * loaded from the classpath; if that fails, a system typeface is used as a fallback.
 */
public final class Skia {

	private static Typeface system;
	private static Typeface harmony;

	private Skia() {
	}

	/** @return a system typeface (Microsoft YaHei if present, else the default). */
	public static Typeface typeface() {
		if (system == null) {
			FontMgr mgr = FontMgr.getDefault();
			system = mgr.matchFamilyStyle("Microsoft YaHei", FontStyle.NORMAL);
			if (system == null) {
				system = mgr.matchFamilyStyle(null, FontStyle.NORMAL);
			}
		}
		return system;
	}

	/** HarmonyOS Sans SC Regular (bundled) — covers Latin + Chinese. */
	public static Typeface harmony() {
		if (harmony == null) {
			harmony = load("HarmonyOS_Sans_SC_Regular.ttf");
		}
		return harmony;
	}

	/** Alias of {@link #harmony()} — only one HarmonyOS weight is bundled to keep the jar small. */
	public static Typeface harmonyMedium() {
		return harmony();
	}

	private static Typeface load(String name) {
		try (InputStream in = Skia.class.getResourceAsStream("/assets/zeroday/fonts/" + name)) {
			if (in != null) {
				Data data = Data.makeFromBytes(in.readAllBytes());
				Typeface typeface = FontMgr.getDefault().makeFromData(data);
				data.close();
				if (typeface != null) {
					return typeface;
				}
			}
		} catch (Exception ignored) {
			// fall back below
		}
		return typeface();
	}
}
