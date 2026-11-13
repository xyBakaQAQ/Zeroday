package com.xybakaqaq.gui.keystrokes;

import java.util.ArrayDeque;

import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.Keystrokes;
import com.xybakaqaq.utils.DragState;
import com.xybakaqaq.utils.skia.Skia;
import com.xybakaqaq.utils.skia.SkijaBridge;

import com.mojang.blaze3d.platform.Window;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

// The Skija-rendered keystrokes overlay: W/A/S/D, the mouse buttons, and optional Space / Shift rows,
// each box lit while its key is held (the press highlight eases in/out). The mouse buttons can show a
// live CPS (clicks-per-second) count. Rendered at the physical framebuffer resolution and draggable
// while the chat bar is open, with its position persisted to windows.json.
public final class KeystrokesWidget {

	private static final String WINDOW = "keystrokes";
	private static final int KEY = 22;
	private static final int GAP = 2;
	private static final int W = KEY * 3 + GAP * 2;
	private static final int BAR_H = 14;

	private static final int ACCENT = 0xFFC20C0C;

	private static final DragState drag = new DragState(WINDOW);

	// per-key press animation (0=up,1=left,2=down,3=right,4=lmb,5=rmb,6=space,7=shift)
	private static final float[] anim = new float[8];
	private static long lastNanos;
	private static long animSeq;

	// CPS click timestamps (ms) + edge state
	private static final ArrayDeque<Long> lmbClicks = new ArrayDeque<>();
	private static final ArrayDeque<Long> rmbClicks = new ArrayDeque<>();
	private static boolean prevLmb;
	private static boolean prevRmb;

	private static final SkijaBridge bridge = new SkijaBridge("keystrokes");
	private static final Font keyFont = new Font(Skia.harmony(), 11f);
	private static final Font smallFont = new Font(Skia.harmony(), 8f);

	private KeystrokesWidget() {
	}

	public static void render(GuiGraphicsExtractor graphics, boolean chatOpen) {
		Keystrokes module = ModuleManager.get(Keystrokes.class);
		boolean showSpace = module == null || module.showSpace();
		boolean showShift = module != null && module.showShift();
		boolean showCps = module != null && module.showCps();

		int h = height(showSpace, showShift);
		drag.update(chatOpen, W, h, graphics.guiWidth(), graphics.guiHeight(), 10, 120);
		int x = drag.x();
		int y = drag.y();

		Minecraft mc = Minecraft.getInstance();
		Options o = mc.options;
		boolean[] down = {
				o.keyUp.isDown(), o.keyLeft.isDown(), o.keyDown.isDown(), o.keyRight.isDown(),
				o.keyAttack.isDown(), o.keyUse.isDown(), o.keyJump.isDown(), o.keyShift.isDown()
		};

		long nowMs = System.currentTimeMillis();
		if (down[4] && !prevLmb) {
			lmbClicks.addLast(nowMs);
		}
		if (down[5] && !prevRmb) {
			rmbClicks.addLast(nowMs);
		}
		prevLmb = down[4];
		prevRmb = down[5];
		prune(lmbClicks, nowMs);
		prune(rmbClicks, nowMs);

		long now = System.nanoTime();
		float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
		lastNanos = now;
		boolean settled = true;
		for (int i = 0; i < anim.length; i++) {
			float target = down[i] ? 1f : 0f;
			anim[i] += (target - anim[i]) * Math.min(1f, dt * 16f);
			if (Math.abs(anim[i] - target) < 0.01f) {
				anim[i] = target;
			} else {
				settled = false;
			}
		}

		String lmb = showCps ? String.valueOf(lmbClicks.size()) : "LMB";
		String rmb = showCps ? String.valueOf(rmbClicks.size()) : "RMB";

		// re-raster every frame only while a highlight is mid-fade; otherwise cache by state
		long key = settled ? SkijaBridge.key(down, lmb, rmb, showSpace, showShift) : ++animSeq;

		Window win = mc.getWindow();
		float scale = win.getGuiScaledWidth() > 0 ? (float) win.getWidth() / win.getGuiScaledWidth() : 1f;
		int sw = Math.max(1, Math.round(W * scale));
		int sh = Math.max(1, Math.round(h * scale));
		try {
			bridge.render(sw, sh, key, canvas -> {
				canvas.scale(scale, scale);
				draw(canvas, o, lmb, rmb, showSpace, showShift, showCps);
			});
			graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), x, y, 0.0F, 0.0F, W, h, sw, sh, sw, sh);
		} catch (Throwable ignored) {
			// skip this frame
		}
	}

	private static int height(boolean showSpace, boolean showShift) {
		int yy = (KEY + GAP) * 2 + KEY + GAP;
		if (showSpace) {
			yy += BAR_H + GAP;
		}
		if (showShift) {
			yy += BAR_H + GAP;
		}
		return yy - GAP;
	}

	private static void draw(Canvas canvas, Options o, String lmb, String rmb,
			boolean showSpace, boolean showShift, boolean showCps) {
		int row1 = KEY + GAP;
		int rowMouse = (KEY + GAP) * 2;
		drawKey(canvas, KEY + GAP, 0, KEY, KEY, label(o.keyUp), null, anim[0], keyFont);
		drawKey(canvas, 0, row1, KEY, KEY, label(o.keyLeft), null, anim[1], keyFont);
		drawKey(canvas, KEY + GAP, row1, KEY, KEY, label(o.keyDown), null, anim[2], keyFont);
		drawKey(canvas, (KEY + GAP) * 2, row1, KEY, KEY, label(o.keyRight), null, anim[3], keyFont);

		int half = (W - GAP) / 2;
		drawKey(canvas, 0, rowMouse, half, KEY, showCps ? "LMB" : lmb, showCps ? lmb : null, anim[4], smallFont);
		drawKey(canvas, half + GAP, rowMouse, W - half - GAP, KEY, showCps ? "RMB" : rmb, showCps ? rmb : null, anim[5], smallFont);

		int yy = rowMouse + KEY + GAP;
		if (showSpace) {
			drawKey(canvas, 0, yy, W, BAR_H, "SPACE", null, anim[6], smallFont);
			yy += BAR_H + GAP;
		}
		if (showShift) {
			drawKey(canvas, 0, yy, W, BAR_H, "SHIFT", null, anim[7], smallFont);
		}
	}

	private static void drawKey(Canvas canvas, float bx, float by, float bw, float bh,
			String label, String sub, float lit, Font font) {
		try (Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			paint.setColor(lerp(0xC81C1C24, ACCENT, lit));
			canvas.drawRRect(RRect.makeXYWH(bx, by, bw, bh, 4), paint);

			int textColor = lerp(0xFFB0B0B8, 0xFFFFFFFF, lit);
			if (sub == null) {
				drawCentered(canvas, font, paint, label, bx, by, bw, bh, textColor);
			} else {
				drawCentered(canvas, font, paint, label, bx, by, bw, bh * 0.52f, textColor);
				drawCentered(canvas, font, paint, sub, bx, by + bh * 0.46f, bw, bh * 0.54f, textColor);
			}
		}
	}

	private static void drawCentered(Canvas canvas, Font font, Paint paint, String s,
			float bx, float by, float bw, float bh, int color) {
		paint.setColor(color);
		FontMetrics m = font.getMetrics();
		float tw = font.measureTextWidth(s);
		float baseline = by + bh / 2f - (m.getAscent() + m.getDescent()) / 2f;
		canvas.drawString(s, bx + (bw - tw) / 2f, baseline, font, paint);
	}

	private static void prune(ArrayDeque<Long> clicks, long nowMs) {
		while (!clicks.isEmpty() && nowMs - clicks.peekFirst() > 1000L) {
			clicks.removeFirst();
		}
	}

	private static String label(KeyMapping key) {
		return key.getTranslatedKeyMessage().getString();
	}

	// Begins a drag if the left button presses inside the widget.
	public static boolean mouseClicked(double mx, double my, int button) {
		return drag.mouseClicked(mx, my, button);
	}

	private static int lerp(int from, int to, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int a = channel(from, to, t, 24);
		int r = channel(from, to, t, 16);
		int g = channel(from, to, t, 8);
		int b = channel(from, to, t, 0);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int channel(int from, int to, float t, int shift) {
		int f = (from >>> shift) & 0xFF;
		int x = (to >>> shift) & 0xFF;
		return Math.round(f + (x - f) * t);
	}
}
