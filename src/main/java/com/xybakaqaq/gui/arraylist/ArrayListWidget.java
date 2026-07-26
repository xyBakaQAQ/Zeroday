package com.xybakaqaq.gui.arraylist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.ModuleManager;
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
import net.minecraft.client.renderer.RenderPipelines;

/**
 * The Skija-rendered ArrayList: the enabled modules' names, longest first, each with a dark backdrop
 * and an accent bar. Defaults to the top-right and is draggable while the chat bar is open. The side
 * is adaptive: on the right half of the screen the rows stay flush right with the bar on the right
 * edge; drag it to the left half and the layout mirrors (flush left, bar on the left edge).
 */
public final class ArrayListWidget {

	private static final String WINDOW = "arraylist";
	private static final int ROW_H = 13;
	private static final int PAD = 5;
	private static final int BAR_W = 2;
	private static final int ACCENT = 0xFFC20C0C;

	private static final DragState drag = new DragState(WINDOW);
	private static final SkijaBridge bridge = new SkijaBridge("hud_arraylist");
	private static final Font font = new Font(Skia.harmony(), 10.5f);

	private ArrayListWidget() {
	}

	public static void render(GuiGraphicsExtractor graphics, boolean chatOpen) {
		List<Module> enabled = new ArrayList<>();
		for (Module module : ModuleManager.getModules()) {
			if (module.isEnabled()) {
				enabled.add(module);
			}
		}
		if (enabled.isEmpty()) {
			return;
		}
		enabled.sort(Comparator.comparingDouble((Module module) -> font.measureTextWidth(module.getName())).reversed());

		String[] names = new String[enabled.size()];
		float maxW = 0f;
		for (int i = 0; i < names.length; i++) {
			names[i] = enabled.get(i).getName();
			maxW = Math.max(maxW, font.measureTextWidth(names[i]));
		}
		int panelW = (int) Math.ceil(maxW) + PAD * 2 + BAR_W;
		int panelH = names.length * ROW_H;

		int guiW = graphics.guiWidth();
		int guiH = graphics.guiHeight();
		drag.update(chatOpen, panelW, panelH, guiW, guiH, guiW - panelW, 0);
		boolean rightSide = drag.x() + panelW / 2 >= guiW / 2;
		int panelX = drag.x();
		int panelY = drag.y();

		long key = SkijaBridge.key(rightSide, (Object) names);

		Window win = Minecraft.getInstance().getWindow();
		float scale = win.getGuiScaledWidth() > 0 ? (float) win.getWidth() / win.getGuiScaledWidth() : 1f;
		int sw = Math.max(1, Math.round(panelW * scale));
		int sh = Math.max(1, Math.round(panelH * scale));
		try {
			bridge.render(sw, sh, key, canvas -> {
				canvas.scale(scale, scale);
				draw(canvas, names, panelW, rightSide);
			});
			graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), panelX, panelY,
					0.0F, 0.0F, panelW, panelH, sw, sh, sw, sh);
		} catch (Throwable ignored) {
			// skip this frame
		}
	}

	private static void draw(Canvas canvas, String[] names, int panelW, boolean rightSide) {
		FontMetrics m = font.getMetrics();
		try (Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			for (int i = 0; i < names.length; i++) {
				String name = names[i];
				float rowW = font.measureTextWidth(name) + PAD * 2;
				float left = rightSide ? panelW - BAR_W - rowW : BAR_W;
				float y = i * ROW_H;
				paint.setColor(0xB0000000);
				canvas.drawRect(Rect.makeXYWH(left, y, rowW, ROW_H), paint);
				paint.setColor(ACCENT);
				canvas.drawRect(Rect.makeXYWH(rightSide ? panelW - BAR_W : 0, y, BAR_W, ROW_H), paint);
				paint.setColor(0xFFFFFFFF);
				float baseline = y + ROW_H / 2f - (m.getAscent() + m.getDescent()) / 2f;
				canvas.drawString(name, left + PAD, baseline, font, paint);
			}
		}
	}

	/** Begins a drag if the left button presses inside the panel (uses the last drawn rect). */
	public static boolean mouseClicked(double mx, double my, int button) {
		return drag.mouseClicked(mx, my, button);
	}
}
