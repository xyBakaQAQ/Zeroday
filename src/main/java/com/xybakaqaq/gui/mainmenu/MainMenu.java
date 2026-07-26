package com.xybakaqaq.gui.mainmenu;

import java.util.ArrayList;
import java.util.List;

import com.xybakaqaq.Zeroday;
import com.xybakaqaq.utils.Sdf;
import com.xybakaqaq.utils.skia.Skia;
import com.xybakaqaq.utils.skia.SkijaBridge;

import com.mojang.blaze3d.platform.Window;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.RRect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;

import net.minecraft.client.renderer.RenderPipelines;

/**
 * Fully custom, Skija-rendered title screen, started from the mod's main class (not a module).
 *
 * <p>Performance: the entire <b>rest</b> menu (gradient + glow + ZERODAY wordmark + button panels +
 * Skija button labels) is rasterised <b>once</b> into a cached texture (re-rendered only on resize /
 * re-layout) and blitted each frame. Hover is then a couple of cheap GPU quads ({@link Sdf}) drawn on
 * top — so the buttons keep crisp Skija (HarmonyOS) text but there is no per-frame Skija raster.
 *
 * <p>The vanilla buttons are re-laid-out into this stack and keep handling clicks; we just draw them.
 */
public final class MainMenu {

	private static final int MENU_X = 46;
	private static final int BTN_W = 200;
	private static final int BTN_H = 22;
	private static final int GAP = 7;

	private static final int TEXT = 0xFFE8E8EA;
	private static final int SUBTEXT = 0xFF9A9AA2;
	private static final int ACCENT = 0xFFC20C0C;

	private static final SkijaBridge cache = new SkijaBridge("mainmenu");
	private static final List<AbstractWidget> buttons = new ArrayList<>();
	private static float[] hover = new float[0];
	private static long lastNanos;
	private static int cacheW = -1;
	private static int cacheH = -1;

	private MainMenu() {
	}

	/** Called once from {@link Zeroday#onInitialize()} — preloads the fonts. */
	public static void init() {
		try {
			Skia.harmonyMedium();
			Skia.harmony();
		} catch (Throwable ignored) {
			// lazy load on first render
		}
		Zeroday.LOGGER.info("Skija main menu started");
	}

	/** Re-lays-out the vanilla title buttons into this menu's vertical stack. Call on init/resize. */
	public static void layout(TitleScreen screen) {
		buttons.clear();
		for (GuiEventListener child : screen.children()) {
			if (child instanceof AbstractWidget widget && widget.visible) {
				buttons.add(widget);
			}
		}
		int n = buttons.size();
		int stackH = n * BTN_H + Math.max(0, n - 1) * GAP;
		int y = Math.max(96, (screen.height - stackH) / 2 + 18);
		for (AbstractWidget widget : buttons) {
			widget.setX(MENU_X);
			widget.setY(y);
			widget.setWidth(BTN_W);
			widget.setHeight(BTN_H);
			y += BTN_H + GAP;
		}
		hover = new float[n];
		cacheW = -1; // re-bake the cached menu after a re-layout
	}

	/** Draws the menu. Call at the head of {@code extractRenderState} (and cancel it). */
	public static void render(TitleScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		long now = System.nanoTime();
		float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
		lastNanos = now;

		blitCache(graphics);

		// Cheap per-frame hover overlays (no Skija raster).
		for (int i = 0; i < buttons.size() && i < hover.length; i++) {
			AbstractWidget widget = buttons.get(i);
			int bx = widget.getX();
			int by = widget.getY();
			int bw = widget.getWidth();
			int bh = widget.getHeight();
			boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
			hover[i] += ((hovered ? 1f : 0f) - hover[i]) * Math.min(1f, dt * 14f);
			float hv = hover[i];
			if (hv > 0.01f) {
				Sdf.roundRect(graphics, bx, by, bw, bh, 6, alpha(0x40FFFFFF, hv));
				Sdf.roundRect(graphics, bx, by, 3, bh, 6, alpha(ACCENT, hv));
			}
		}
	}

	private static void blitCache(GuiGraphicsExtractor graphics) {
		int guiW = graphics.guiWidth();
		int guiH = graphics.guiHeight();
		Window win = Minecraft.getInstance().getWindow();
		// Cap the cached texture at 2x the GUI resolution: crisp at 1080p, cheap on 4K.
		float fullScale = guiW > 0 ? (float) win.getWidth() / guiW : 1f;
		float renderScale = Math.min(2.0f, fullScale);
		int sw = Math.max(1, Math.round(guiW * renderScale));
		int sh = Math.max(1, Math.round(guiH * renderScale));
		if (sw != cacheW || sh != cacheH) {
			try {
				cache.render(sw, sh, canvas -> {
					canvas.scale(renderScale, renderScale);
					drawMenu(canvas, guiW, guiH);
				});
				cacheW = sw;
				cacheH = sh;
			} catch (Throwable ignored) {
				return;
			}
		}
		if (cacheW < 0) {
			return;
		}
		graphics.blit(RenderPipelines.GUI_TEXTURED, cache.textureId(), 0, 0, 0.0F, 0.0F, guiW, guiH, sw, sh, sw, sh);
	}

	/** Bakes the whole rest menu once (background + title + button panels + labels). */
	private static void drawMenu(Canvas canvas, int w, int h) {
		try (Paint bg = new Paint()) {
			bg.setShader(Shader.makeLinearGradient(0, 0, w, h, new int[] {0xFF1C1422, 0xFF0B0B11}));
			canvas.drawRRect(RRect.makeXYWH(0, 0, w, h, 0), bg);
		}
		try (Paint glow = new Paint()) {
			glow.setShader(Shader.makeRadialGradient(MENU_X + 60, h * 0.32f, h * 0.75f, new int[] {0x3AC20C0C, 0x00000000}));
			canvas.drawRRect(RRect.makeXYWH(0, 0, w, h, 0), glow);
		}

		float titleBaseline = buttons.isEmpty() ? h * 0.32f : buttons.get(0).getY() - 22;
		try (Font title = new Font(Skia.harmonyMedium(), 34f); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			paint.setColor(0xFFF2F2F4);
			canvas.drawString("ZERODAY", MENU_X, titleBaseline, title, paint);
			paint.setColor(ACCENT);
			canvas.drawRRect(RRect.makeXYWH(MENU_X + 1, titleBaseline + 6, 132, 2, 1), paint);
		}

		try (Font label = new Font(Skia.harmony(), 13f); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			FontMetrics metrics = label.getMetrics();
			for (AbstractWidget widget : buttons) {
				int bx = widget.getX();
				int by = widget.getY();
				int bw = widget.getWidth();
				int bh = widget.getHeight();
				paint.setColor(0x66141420);
				canvas.drawRRect(RRect.makeXYWH(bx, by, bw, bh, 6), paint);
				float baseline = by + bh / 2f - (metrics.getAscent() + metrics.getDescent()) / 2f;
				paint.setColor(SUBTEXT);
				canvas.drawString(widget.getMessage().getString(), bx + 14, baseline, label, paint);
			}
		}
	}

	private static int alpha(int color, float factor) {
		int a = Math.round((color >>> 24) * Math.max(0f, Math.min(1f, factor)));
		return (a << 24) | (color & 0xFFFFFF);
	}
}
