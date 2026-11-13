package com.xybakaqaq.gui.mainmenu;

import java.util.IdentityHashMap;
import java.util.Map;

import com.xybakaqaq.utils.skia.Skia;
import com.xybakaqaq.utils.skia.SkijaBridge;

import com.mojang.blaze3d.platform.Window;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;

// One cached Skija label texture per widget, re-baked only when its text, size or colour changes.
final class MenuLabel {

	private static final int LIMIT = 64;

	private static final Map<AbstractWidget, SkijaBridge> BRIDGES = new IdentityHashMap<>();
	private static Screen owner;
	private static int nextId;

	private MenuLabel() {
	}

	// Drops the textures of a closed/resized screen. Call once per frame, before widgets render.
	static void beginFrame(Screen screen) {
		if (owner == screen && BRIDGES.size() <= LIMIT) {
			return;
		}
		BRIDGES.values().forEach(SkijaBridge::close);
		BRIDGES.clear();
		owner = screen;
	}

	// Text centred in the widget, sized from its height (buttons, sliders).
	static void drawCentered(GuiGraphicsExtractor graphics, AbstractWidget widget, String text, int color) {
		draw(graphics, widget, text, color, Math.max(8.5f, Math.min(13f, widget.getHeight() * 0.58f)), 0.5f, 0f);
	}

	// Text laid out inside the widget: align 0 = left, 0.5 = centred, 1 = right.
	static void draw(GuiGraphicsExtractor graphics, AbstractWidget widget, String text, int color,
			float size, float align, float inset) {
		int w = widget.getWidth();
		int h = widget.getHeight();
		if (w <= 0 || h <= 0 || text.isEmpty()) {
			return;
		}

		int guiW = graphics.guiWidth();
		Window win = Minecraft.getInstance().getWindow();
		float scale = Math.min(2f, guiW > 0 ? (float) win.getWidth() / guiW : 1f);
		int tw = Math.max(1, Math.round(w * scale));
		int th = Math.max(1, Math.round(h * scale));
		long key = SkijaBridge.key(text, tw, th, color, size, align, inset);

		SkijaBridge bridge = BRIDGES.get(widget);
		if (bridge == null) {
			bridge = new SkijaBridge("menu_label_" + nextId++);
			BRIDGES.put(widget, bridge);
		}

		try {
			bridge.render(tw, th, key, canvas -> draw(canvas, w, h, scale, text, color, size, align, inset));
			graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), widget.getX(), widget.getY(),
					0.0F, 0.0F, w, h, tw, th, tw, th);
		} catch (Throwable ignored) {
			// fall through: unlabelled widgets are still usable
		}
	}

	private static void draw(Canvas canvas, int w, int h, float scale, String text, int color,
			float size, float align, float inset) {
		canvas.scale(scale, scale);
		try (Font font = new Font(Skia.harmony(), size); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			paint.setColor(color);
			FontMetrics metrics = font.getMetrics();
			float baseline = h / 2f - (metrics.getAscent() + metrics.getDescent()) / 2f;
			float room = Math.max(0f, w - inset * 2f - font.measureTextWidth(text));
			canvas.drawString(text, inset + room * align, baseline, font, paint);
		}
	}
}
