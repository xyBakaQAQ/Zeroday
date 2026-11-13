package com.xybakaqaq.gui.mainmenu;

import java.util.ArrayList;
import java.util.List;

import com.xybakaqaq.Zeroday;
import com.xybakaqaq.config.UiConfig;
import com.xybakaqaq.mixin.ScreenAccessor;
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

// Custom Skija title screen. The menu is baked into one cached texture (gradient, wordmark, button
// panels and labels) and blitted each frame; hover is a cheap GPU overlay. The vanilla buttons are
// re-laid-out into the stack and keep handling clicks.
public final class MainMenu {

	// Layout at scale 1; layout() shrinks it to fit small windows.
	private static final int MENU_X = 46;
	private static final int BTN_W = 200;
	private static final int BTN_H = 22;
	private static final int GAP = 7;
	private static final float TITLE_SIZE = 34f;
	private static final float LABEL_SIZE = 13f;
	private static final float UNDERLINE_W = 132f;
	private static final int TITLE_BLOCK = 60;

	private static final int TEXT = 0xFFE8E8EA;
	private static final int SUBTEXT = 0xFF9A9AA2;
	private static final int ACCENT = 0xFFC20C0C;

	private static final int TOGGLE_W = 112;
	private static final int TOGGLE_H = 18;

	private static final SkijaBridge cache = new SkijaBridge("mainmenu");
	private static final SkijaBridge toggleCache = new SkijaBridge("mainmenu_toggle");
	private static final List<AbstractWidget> buttons = new ArrayList<>();
	private static int cacheW = -1;
	private static int cacheH = -1;

	// Resolved by layout().
	private static float scale = 1f;
	private static int menuX = MENU_X;

	private MainMenu() {
	}

	// Called once from Zeroday.onInitialize() to preload the fonts.
	public static void init() {
		try {
			Skia.harmonyMedium();
			Skia.harmony();
		} catch (Throwable ignored) {
			// lazy load on first render
		}
		Zeroday.LOGGER.info("Skija main menu started");
	}

	// Re-lays-out the vanilla title buttons into this menu's vertical stack. Call on init/resize.
	public static void layout(TitleScreen screen) {
		buttons.clear();
		for (GuiEventListener child : screen.children()) {
			if (child instanceof AbstractWidget widget && widget.visible) {
				buttons.add(widget);
			}
		}
		int n = buttons.size();
		if (n == 0) {
			return;
		}

		// Shrink everything so the whole stack (with the wordmark above it) fits the window.
		int margin = 12;
		float stackH = n * BTN_H + Math.max(0, n - 1) * GAP;
		float fitH = (screen.height - margin * 2) / (stackH + TITLE_BLOCK);
		float fitW = (screen.width - margin * 2) / (float) (MENU_X + BTN_W);
		scale = Math.max(0.3f, Math.min(1f, Math.min(fitH, fitW)));

		int btnW = Math.max(80, Math.round(BTN_W * scale));
		int btnH = Math.max(10, Math.round(BTN_H * scale));
		int gap = Math.max(1, Math.round(GAP * scale));
		int stack = n * btnH + Math.max(0, n - 1) * gap;
		int titleBlock = Math.max(20, Math.round(TITLE_BLOCK * scale));
		menuX = Math.max(margin, Math.min(Math.round(MENU_X * scale), screen.width - btnW - margin));

		int y = Math.max(titleBlock, (screen.height - stack) / 2 + Math.round(16 * scale));
		y = Math.min(y, Math.max(titleBlock, screen.height - margin - stack));
		for (AbstractWidget widget : buttons) {
			widget.setX(menuX);
			widget.setY(y);
			widget.setWidth(btnW);
			widget.setHeight(btnH);
			y += btnH + gap;
		}
		cacheW = -1; // re-bake the cached menu after a re-layout
	}

	// Draws the menu; the screen's own render state is cancelled by TitleScreenMixin.
	public static void render(TitleScreen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		MenuBackdrop.draw(graphics);
		blitCache(graphics);

		int radius = Math.max(2, Math.round(6 * scale));
		int bar = Math.max(2, Math.round(3 * scale));
		for (AbstractWidget widget : buttons) {
			int bx = widget.getX();
			int by = widget.getY();
			int bw = widget.getWidth();
			int bh = widget.getHeight();
			boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
			float hv = MenuAnimation.hover(widget, hovered);
			if (hv > 0.01f) {
				Sdf.roundRect(graphics, bx, by, bw, bh, radius, alpha(0x40FFFFFF, hv));
				Sdf.roundRect(graphics, bx, by, bar, bh, radius, alpha(ACCENT, hv));
			}
		}
		drawToggle(graphics, mouseX, mouseY);
	}

	// Draws only the custom/vanilla switch (also used while the vanilla menu is showing).
	public static void drawToggle(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int guiW = graphics.guiWidth();
		int guiH = graphics.guiHeight();
		int x = guiW - TOGGLE_W - 10;
		int y = guiH - TOGGLE_H - 10;
		boolean custom = UiConfig.customMenu();

		Window win = Minecraft.getInstance().getWindow();
		float renderScale = Math.min(2f, guiW > 0 ? (float) win.getWidth() / guiW : 1f);
		int sw = Math.max(1, Math.round(TOGGLE_W * renderScale));
		int sh = Math.max(1, Math.round(TOGGLE_H * renderScale));
		try {
			toggleCache.render(sw, sh, SkijaBridge.key(custom, sw, sh), canvas -> {
				canvas.scale(renderScale, renderScale);
				try (Paint paint = new Paint()) {
					paint.setAntiAlias(true);
					paint.setColor(0x66141420);
					canvas.drawRRect(RRect.makeXYWH(0, 0, TOGGLE_W, TOGGLE_H, 9), paint);
					paint.setColor(custom ? ACCENT : 0xFF4A4A52);
					canvas.drawRRect(RRect.makeXYWH(8, TOGGLE_H / 2f - 2, 4, 4, 2), paint);
					try (Font font = new Font(Skia.harmony(), 9.5f)) {
						FontMetrics metrics = font.getMetrics();
						paint.setColor(custom ? TEXT : SUBTEXT);
						canvas.drawString(custom ? "Custom Menu" : "Vanilla Menu", 18,
								TOGGLE_H / 2f - (metrics.getAscent() + metrics.getDescent()) / 2f, font, paint);
					}
				}
			});
			graphics.blit(RenderPipelines.GUI_TEXTURED, toggleCache.textureId(), x, y, 0.0F, 0.0F,
					TOGGLE_W, TOGGLE_H, sw, sh, sw, sh);
		} catch (Throwable ignored) {
			// keep the vanilla screen usable even if Skija fails
		}

		if (mouseX >= x && mouseX < x + TOGGLE_W && mouseY >= y && mouseY < y + TOGGLE_H) {
			Sdf.roundRect(graphics, x, y, TOGGLE_W, TOGGLE_H, 9, 0x33FFFFFF);
		}
	}

	// True when the click hit the switch (and the mode was flipped).
	public static boolean toggleClicked(TitleScreen screen, double mouseX, double mouseY) {
		Minecraft mc = Minecraft.getInstance();
		int x = mc.getWindow().getGuiScaledWidth() - TOGGLE_W - 10;
		int y = mc.getWindow().getGuiScaledHeight() - TOGGLE_H - 10;
		if (mouseX < x || mouseX >= x + TOGGLE_W || mouseY < y || mouseY >= y + TOGGLE_H) {
			return false;
		}
		UiConfig.setCustomMenu(!UiConfig.customMenu());
		// Rebuild the title screen so the widgets return to (or leave) the vanilla layout.
		((ScreenAccessor) screen).zeroday$rebuildWidgets();
		return true;
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

	// Bakes the whole rest menu once (background + title + button panels + labels).
	private static void drawMenu(Canvas canvas, int w, int h) {
		float titleSize = Math.max(14f, TITLE_SIZE * scale);
		float titleBaseline = buttons.isEmpty() ? h * 0.32f : buttons.get(0).getY() - Math.round(22 * scale);
		try (Font title = new Font(Skia.harmonyMedium(), titleSize); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			paint.setColor(0xFFF2F2F4);
			canvas.drawString("ZERODAY", menuX, titleBaseline, title, paint);
			paint.setColor(ACCENT);
			canvas.drawRRect(RRect.makeXYWH(menuX + 1, titleBaseline + Math.max(4, Math.round(6 * scale)),
					Math.max(40, UNDERLINE_W * scale), Math.max(1, Math.round(2 * scale)), 1), paint);
		}

		float labelSize = Math.max(8f, LABEL_SIZE * scale);
		int radius = Math.max(2, Math.round(6 * scale));
		float pad = Math.max(6f, Math.round(14 * scale));
		try (Font label = new Font(Skia.harmony(), labelSize); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			FontMetrics metrics = label.getMetrics();
			for (AbstractWidget widget : buttons) {
				int bx = widget.getX();
				int by = widget.getY();
				int bw = widget.getWidth();
				int bh = widget.getHeight();
				paint.setColor(0x66141420);
				canvas.drawRRect(RRect.makeXYWH(bx, by, bw, bh, radius), paint);
				float baseline = by + bh / 2f - (metrics.getAscent() + metrics.getDescent()) / 2f;
				paint.setColor(SUBTEXT);
				canvas.drawString(widget.getMessage().getString(), bx + pad, baseline, label, paint);
			}
		}
	}

	private static int alpha(int color, float factor) {
		int a = Math.round((color >>> 24) * Math.max(0f, Math.min(1f, factor)));
		return (a << 24) | (color & 0xFFFFFF);
	}
}
