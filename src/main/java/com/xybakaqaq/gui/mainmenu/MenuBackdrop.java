package com.xybakaqaq.gui.mainmenu;

import com.xybakaqaq.utils.skia.SkijaBridge;

import com.mojang.blaze3d.platform.Window;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.RRect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

// Animated menu backdrop: a baked gradient plus two soft glows that drift slowly across the screen.
public final class MenuBackdrop {

	private static final int ORB_SIZE = 256;

	private static final SkijaBridge gradient = new SkijaBridge("menu_gradient");
	private static final SkijaBridge orb = new SkijaBridge("menu_orb");

	private MenuBackdrop() {
	}

	public static void draw(GuiGraphicsExtractor graphics) {
		int guiW = graphics.guiWidth();
		int guiH = graphics.guiHeight();
		if (guiW <= 0 || guiH <= 0) {
			return;
		}

		Window win = Minecraft.getInstance().getWindow();
		float renderScale = Math.min(2f, (float) win.getWidth() / guiW);
		int sw = Math.max(1, Math.round(guiW * renderScale));
		int sh = Math.max(1, Math.round(guiH * renderScale));

		try {
			gradient.render(sw, sh, SkijaBridge.key(sw, sh), canvas -> {
				canvas.scale(renderScale, renderScale);
				drawGradient(canvas, guiW, guiH);
			});
			graphics.blit(RenderPipelines.GUI_TEXTURED, gradient.textureId(), 0, 0, 0.0F, 0.0F,
					guiW, guiH, sw, sh, sw, sh);

			orb.render(ORB_SIZE, ORB_SIZE, MenuBackdrop::drawOrb);
			float seconds = (System.nanoTime() % 1_000_000_000_000L) / 1.0e9f;
			drawOrb(graphics, guiW, guiH, seconds, 0.28f, 0.30f, 1.15f, 0.11f, 0.17f, 0.20f);
			drawOrb(graphics, guiW, guiH, seconds, 0.74f, 0.68f, 0.85f, -0.09f, 0.13f, 0.16f);
		} catch (Throwable ignored) {
			// a missing backdrop should not take the menu down with it
		}
	}

	private static void drawGradient(Canvas canvas, int w, int h) {
		try (Paint paint = new Paint()) {
			paint.setShader(Shader.makeLinearGradient(0, 0, w, h, new int[] {0xFF1C1422, 0xFF0B0B11}));
			canvas.drawRRect(RRect.makeXYWH(0, 0, w, h, 0), paint);
		}
	}

	private static void drawOrb(Canvas canvas) {
		canvas.clear(0x00000000);
		try (Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			paint.setShader(Shader.makeRadialGradient(ORB_SIZE / 2f, ORB_SIZE / 2f, ORB_SIZE / 2f,
					new int[] {0x30C20C0C, 0x00000000}));
			canvas.drawRRect(RRect.makeXYWH(0, 0, ORB_SIZE, ORB_SIZE, 0), paint);
		}
	}

	private static void drawOrb(GuiGraphicsExtractor graphics, int w, int h, float seconds,
			float fx, float fy, float sizeFactor, float fxSpeed, float fySpeed, float offset) {
		int size = Math.max(1, Math.round(h * sizeFactor));
		int cx = Math.round(w * fx + (float) Math.sin(seconds * fxSpeed + offset) * w * 0.22f);
		int cy = Math.round(h * fy + (float) Math.cos(seconds * fySpeed + offset) * h * 0.26f);
		graphics.blit(RenderPipelines.GUI_TEXTURED, orb.textureId(), cx - size / 2, cy - size / 2,
				0.0F, 0.0F, size, size, ORB_SIZE, ORB_SIZE, ORB_SIZE, ORB_SIZE);
	}
}
