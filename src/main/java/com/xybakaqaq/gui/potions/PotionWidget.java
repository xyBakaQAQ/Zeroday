package com.xybakaqaq.gui.potions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.xybakaqaq.utils.DragState;
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
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * The Skija-rendered potion-effect overlay: one row per active effect (color bar + MC icon + name +
 * remaining time), defaulting to the bottom-right and draggable while the chat bar is open. The
 * color bar flips to the outer screen edge like the ArrayList's: bar on the left edge in the left
 * half of the screen, on the right edge in the right half. The vanilla effect display is suppressed
 * by {@code HudMixin} while the HUD's Potions setting is on.
 */
public final class PotionWidget {

	private static final String WINDOW = "potions";
	private static final int ROW = 20;
	private static final int ICON = 16;
	private static final int PAD = 4;
	private static final int GAP = 4;
	private static final int BAR = 2;

	private static final DragState drag = new DragState(WINDOW);
	private static final SkijaBridge bridge = new SkijaBridge("hud_potions");
	private static final Font nameFont = new Font(Skia.harmony(), 10.5f);
	private static final Font durFont = new Font(Skia.harmony(), 8f);

	private PotionWidget() {
	}

	public static void render(GuiGraphicsExtractor graphics, boolean chatOpen) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
		if (effects.isEmpty()) {
			return;
		}
		effects.sort(Comparator.comparingInt(e -> e.getEffect().value().isBeneficial() ? 0 : 1));

		int n = effects.size();
		String[] names = new String[n];
		String[] durs = new String[n];
		float maxText = 0f;
		for (int i = 0; i < n; i++) {
			MobEffectInstance effect = effects.get(i);
			int amp = effect.getAmplifier();
			names[i] = effect.getEffect().value().getDisplayName().getString() + (amp > 0 ? " " + (amp + 1) : "");
			durs[i] = durationText(effect);
			maxText = Math.max(maxText, Math.max(nameFont.measureTextWidth(names[i]), durFont.measureTextWidth(durs[i])));
		}
		int panelW = BAR + PAD + ICON + GAP + (int) Math.ceil(maxText) + PAD;
		int panelH = n * ROW;

		int guiW = graphics.guiWidth();
		int guiH = graphics.guiHeight();
		drag.update(chatOpen, panelW, panelH, guiW, guiH, guiW - panelW - 4, guiH - panelH - 4);
		boolean rightSide = drag.x() + panelW / 2 >= guiW / 2;
		int panelX = drag.x();
		int panelY = drag.y();

		long key = SkijaBridge.key(rightSide, names, durs);

		Window win = mc.getWindow();
		float scale = win.getGuiScaledWidth() > 0 ? (float) win.getWidth() / win.getGuiScaledWidth() : 1f;
		int sw = Math.max(1, Math.round(panelW * scale));
		int sh = Math.max(1, Math.round(panelH * scale));
		try {
			bridge.render(sw, sh, key, canvas -> {
				canvas.scale(scale, scale);
				draw(canvas, effects, names, durs, panelW, rightSide);
			});
			graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), panelX, panelY,
					0.0F, 0.0F, panelW, panelH, sw, sh, sw, sh);
		} catch (Throwable ignored) {
			return;
		}

		for (int i = 0; i < n; i++) {
			Identifier sprite = Hud.getMobEffectSprite(effects.get(i).getEffect());
			int ix = panelX + BAR + PAD;
			int iy = panelY + i * ROW + (ROW - ICON) / 2;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, ix, iy, ICON, ICON);
		}
	}

	private static void draw(Canvas canvas, List<MobEffectInstance> effects, String[] names, String[] durs,
			int panelW, boolean rightSide) {
		FontMetrics nm = nameFont.getMetrics();
		FontMetrics dm = durFont.getMetrics();
		float barX = rightSide ? panelW - BAR : 0;
		float textX = BAR + PAD + ICON + GAP;
		try (Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			for (int i = 0; i < effects.size(); i++) {
				float y = i * ROW;
				MobEffect effect = effects.get(i).getEffect().value();
				paint.setColor(0xB0000000);
				canvas.drawRect(Rect.makeXYWH(0, y, panelW, ROW), paint);
				paint.setColor(0xFF000000 | (effect.getColor() & 0xFFFFFF));
				canvas.drawRect(Rect.makeXYWH(barX, y, BAR, ROW), paint);
				paint.setColor(0xFFFFFFFF);
				canvas.drawString(names[i], textX, y + ROW * 0.30f - (nm.getAscent() + nm.getDescent()) / 2f, nameFont, paint);
				paint.setColor(0xFFB8B8C0);
				canvas.drawString(durs[i], textX, y + ROW * 0.72f - (dm.getAscent() + dm.getDescent()) / 2f, durFont, paint);
			}
		}
	}

	/** Begins a drag if the left button presses inside the panel (uses the last drawn rect). */
	public static boolean mouseClicked(double mx, double my, int button) {
		return drag.mouseClicked(mx, my, button);
	}

	private static String durationText(MobEffectInstance effect) {
		if (effect.isInfiniteDuration()) {
			return "∞";
		}
		int s = effect.getDuration() / 20;
		int sec = s % 60;
		return (s / 60) + ":" + (sec < 10 ? "0" : "") + sec;
	}
}
