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
import io.github.humbleui.types.Rect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;

// Skija text field content: value or hint, selection, horizontal scroll and the caret. The vanilla
// EditBox still owns all input handling; only its pixels are replaced. One small texture is cached and
// re-baked only when the text/cursor/selection changes, so typing never re-bakes the whole page.
public final class MenuTextField {

	private static final int TEXT = 0xFFE8E8EA;
	private static final int HINT = 0xFF8A8A92;
	private static final int SELECTION = 0x50C20C0C;
	private static final int CARET = 0xFFC20C0C;
	private static final int RULE = 0x33FFFFFF;
	private static final int ACCENT = 0xFFC20C0C;
	private static final long BLINK_MS = 900L;

	private static final SkijaBridge bridge = new SkijaBridge("menu_text_field");

	// Caret position in widget space, written by the bake so the caret can be drawn as a live quad.
	private static final float[] caret = { -1f };

	private MenuTextField() {
	}

	// The field as the custom pages show it: no frame, just the text on the backdrop with a hairline
	// underline that turns into an accent bar while the field has focus.
	public static void drawField(GuiGraphicsExtractor graphics, EditBox box) {
		draw(graphics, box);
		int x = box.getX();
		int lineY = box.getY() + box.getHeight() - 1;
		int lineW = box.getWidth();
		float focus = MenuAnimation.hover(box, box.isFocused());
		Sdf.roundRect(graphics, x, lineY, lineW, 1, 0, RULE);
		if (focus > 0.01f) {
			Sdf.roundRect(graphics, x, lineY - 1, Math.max(1, Math.round(lineW * focus)), 2, 0,
					alpha(ACCENT, focus));
		}
	}

	private static int alpha(int color, float factor) {
		int a = Math.round((color >>> 24) * Math.max(0f, Math.min(1f, factor)));
		return (a << 24) | (color & 0xFFFFFF);
	}

	static void draw(GuiGraphicsExtractor graphics, EditBox box) {
		int w = box.getWidth();
		int h = box.getHeight();
		if (w <= 0 || h <= 0) {
			return;
		}

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

		int guiW = graphics.guiWidth();
		Window win = Minecraft.getInstance().getWindow();
		float scale = Math.min(2f, guiW > 0 ? (float) win.getWidth() / guiW : 1f);
		int tw = Math.max(1, Math.round(w * scale));
		int th = Math.max(1, Math.round(h * scale));
		long key = SkijaBridge.key(text, tw, th, color, size, cursor, highlight);

		caret[0] = -1f;
		try {
			bridge.render(tw, th, key, canvas -> paint(canvas, w, h, scale, text, color, size, cursor, highlight));
			graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), box.getX(), box.getY(),
					0.0F, 0.0F, w, h, tw, th, tw, th);
		} catch (Throwable ignored) {
			return;
		}

		// Drawn live so it can blink without re-baking anything.
		if (focused && caret[0] >= 0f && (System.currentTimeMillis() / BLINK_MS) % 2L == 0L) {
			Sdf.roundRect(graphics, box.getX() + Math.round(caret[0]), box.getY() + 3, 1, h - 6, 0, CARET);
		}
	}

	private static void paint(Canvas canvas, int w, int h, float scale, String text, int color,
			float size, int cursor, int highlight) {
		canvas.scale(scale, scale);
		try (Font font = new Font(Skia.harmony(), size); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			FontMetrics metrics = font.getMetrics();
			float baseline = h / 2f - (metrics.getAscent() + metrics.getDescent()) / 2f;

			// Keep the caret in view: scroll just enough, and never past the end of the text.
			float innerW = w - 2f;
			float caretX = cursor >= 0 ? font.measureTextWidth(text.substring(0, cursor)) : 0f;
			float offset = Math.max(0f, caretX - innerW);
			if (offset > 0f) {
				offset = Math.min(offset, Math.max(0f, font.measureTextWidth(text) - innerW));
			}

			if (cursor >= 0 && highlight >= 0 && cursor != highlight) {
				int from = Math.min(cursor, highlight);
				int to = Math.max(cursor, highlight);
				float x0 = font.measureTextWidth(text.substring(0, from)) - offset;
				float x1 = font.measureTextWidth(text.substring(0, to)) - offset;
				paint.setColor(SELECTION);
				canvas.drawRect(Rect.makeXYWH(x0, 2, Math.max(1, x1 - x0), h - 4), paint);
			}

			paint.setColor(color);
			canvas.drawString(text, -offset, baseline, font, paint);

			caret[0] = cursor >= 0 ? caretX - offset : -1f;
		}
	}
}
