package com.xybakaqaq.gui.mainmenu;

import com.xybakaqaq.mixin.EditBoxAccessor;
import com.xybakaqaq.utils.Sdf;
import com.xybakaqaq.utils.skia.Skia;
import com.xybakaqaq.utils.skia.SkijaBridge;

import com.mojang.blaze3d.platform.Window;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.Rect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;

// Skija text field content: value or hint, selection, horizontal scroll and the caret. The vanilla
// EditBox still owns all input handling; only its pixels are replaced. One small texture is cached and
// re-baked only when the text/cursor/selection changes, so typing never re-bakes the whole page.
public final class MenuTextField {

	private static final int TEXT = 0xFFF2F2F4;
	private static final int HINT = 0xFF9AA0A8;
	private static final int ICON = 0xFF9AA0A8;
	private static final int SELECTION = 0x50C20C0C;
	private static final int CARET = 0xFFC20C0C;
	private static final int RULE = 0x40FFFFFF;
	private static final int ACCENT = 0xFFC20C0C;
	private static final int FIELD_TOP = 0x6B141420;
	private static final int FIELD_BOTTOM = 0x5A101018;
	private static final int FIELD_TOP_FOCUS = 0x8A1C1C28;
	private static final int FIELD_BOTTOM_FOCUS = 0x7A16161F;

	// Text inset: a magnifier on the left, then the text, then a small gap on the right.
	private static final float PAD_LEFT = 8f;
	private static final float ICON_W = 13f;
	private static final float PAD_RIGHT = 8f;

	private static final long BLINK_MS = 900L;

	private static final SkijaBridge bridge = new SkijaBridge("menu_text_field");

	// Caret position in widget space, written by the bake so the caret can be drawn as a live quad.
	private static final float[] caret = { -1f };

	private MenuTextField() {
	}

	// The field as the custom pages show it: a soft rounded face with a magnifier, the text, and a
	// hairline underline that turns into an accent bar while the field has focus.
	public static void drawField(GuiGraphicsExtractor graphics, EditBox box) {
		int x = box.getX();
		int y = box.getY();
		int w = box.getWidth();
		int h = box.getHeight();
		if (w <= 0 || h <= 0) {
			return;
		}
		float focus = MenuAnimation.hover(box, box.isFocused());
		int radius = Math.max(3, Math.min(6, h / 4));

		// Face: dark enough to read as an input on the backdrop, brighter while focused.
		Sdf.roundRect(graphics, x, y, w, h, radius,
				mix(FIELD_TOP, FIELD_TOP_FOCUS, focus), mix(FIELD_BOTTOM, FIELD_BOTTOM_FOCUS, focus));
		draw(graphics, box, focus);

		// Hairline base, and the accent bar that grows out of it from the left while focused.
		Sdf.roundRect(graphics, x + 3, y + h - 1, Math.max(1, w - 6), 1, 0, RULE);
		if (focus > 0.01f) {
			int barW = Math.max(1, Math.round((w - 6) * Math.min(1f, focus)));
			Sdf.roundRect(graphics, x + 3, y + h - 2, barW, 2, 1, alpha(ACCENT, focus));
		}
	}

	private static int alpha(int color, float factor) {
		int a = Math.round((color >>> 24) * Math.max(0f, Math.min(1f, factor)));
		return (a << 24) | (color & 0xFFFFFF);
	}

	private static int mix(int from, int to, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int a = lerpChannel(from, to, t, 24);
		int r = lerpChannel(from, to, t, 16);
		int g = lerpChannel(from, to, t, 8);
		int b = lerpChannel(from, to, t, 0);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int lerpChannel(int from, int to, float t, int shift) {
		int f = (from >>> shift) & 0xFF;
		int x = (to >>> shift) & 0xFF;
		return Math.round(f + (x - f) * t);
	}

	private static void draw(GuiGraphicsExtractor graphics, EditBox box, float focus) {
		int w = box.getWidth();
		int h = box.getHeight();

		EditBoxAccessor accessor = (EditBoxAccessor) box;
		String value = box.getValue();
		boolean hasValue = !value.isEmpty();
		boolean focused = box.isFocused();
		// The hint only shows while the field is idle; once focused the caret sits on an empty line.
		String hint = accessor.zeroday$hint() == null ? "" : accessor.zeroday$hint().getString();
		String text = hasValue ? value : (focused ? "" : hint);
		int color = hasValue ? TEXT : HINT;
		float size = Math.max(8f, Math.min(9.5f, h * 0.5f));
		int cursor = hasValue ? Math.min(box.getCursorPosition(), text.length()) : (focused ? 0 : -1);
		int highlight = hasValue ? Math.min(accessor.zeroday$highlightPos(), text.length()) : -1;
		boolean accentIcon = focus > 0.5f;

		int guiW = graphics.guiWidth();
		Window win = Minecraft.getInstance().getWindow();
		float scale = Math.min(2f, guiW > 0 ? (float) win.getWidth() / guiW : 1f);
		int tw = Math.max(1, Math.round(w * scale));
		int th = Math.max(1, Math.round(h * scale));
		long key = SkijaBridge.key(text, tw, th, color, size, cursor, highlight, accentIcon);

		caret[0] = -1f;
		try {
			bridge.render(tw, th, key, canvas -> paint(canvas, w, h, scale, text, color, size, cursor, highlight, accentIcon));
			graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), box.getX(), box.getY(),
					0.0F, 0.0F, w, h, tw, th, tw, th);
		} catch (Throwable ignored) {
			return;
		}

		// Drawn live so it can blink without re-baking anything.
		if (focused && caret[0] >= 0f && (System.currentTimeMillis() / BLINK_MS) % 2L == 0L) {
			Sdf.roundRect(graphics, box.getX() + Math.round(caret[0]), box.getY() + 4, 1, h - 8, 0, CARET);
		}
	}

	private static void paint(Canvas canvas, int w, int h, float scale, String text, int color,
			float size, int cursor, int highlight, boolean accentIcon) {
		canvas.scale(scale, scale);
		try (Font font = new Font(Skia.harmony(), size); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			FontMetrics metrics = font.getMetrics();
			float baseline = h / 2f - (metrics.getAscent() + metrics.getDescent()) / 2f;
			float padLeft = PAD_LEFT + ICON_W;
			float innerW = Math.max(1f, w - padLeft - PAD_RIGHT);

			// Keep the caret in view: scroll just enough, and never past the end of the text.
			float caretX = cursor >= 0 ? font.measureTextWidth(text.substring(0, cursor)) : 0f;
			float offset = Math.max(0f, caretX - innerW);
			if (offset > 0f) {
				offset = Math.min(offset, Math.max(0f, font.measureTextWidth(text) - innerW));
			}

			if (cursor >= 0 && highlight >= 0 && cursor != highlight) {
				int from = Math.min(cursor, highlight);
				int to = Math.max(cursor, highlight);
				float x0 = padLeft + font.measureTextWidth(text.substring(0, from)) - offset;
				float x1 = padLeft + font.measureTextWidth(text.substring(0, to)) - offset;
				paint.setColor(SELECTION);
				canvas.drawRect(Rect.makeXYWH(x0, 3, Math.max(1, x1 - x0), h - 6), paint);
			}

			// Magnifier: a ring plus a short handle, in the accent colour while focused.
			float cx = PAD_LEFT + 4.2f;
			float cy = h / 2f - 1.6f;
			paint.setMode(PaintMode.STROKE);
			paint.setStrokeWidth(1.4f);
			paint.setColor(accentIcon ? ACCENT : ICON);
			canvas.drawCircle(cx, cy, 3.1f, paint);
			canvas.drawLine(cx + 2.2f, cy + 2.2f, cx + 5.1f, cy + 5.1f, paint);
			paint.setMode(PaintMode.FILL);
			paint.setStrokeWidth(1f);

			paint.setColor(color);
			canvas.drawString(text, padLeft - offset, baseline, font, paint);

			caret[0] = cursor >= 0 ? padLeft + caretX - offset : -1f;
		}
	}
}
