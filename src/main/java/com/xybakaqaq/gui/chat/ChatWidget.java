package com.xybakaqaq.gui.chat;

import java.util.ArrayList;
import java.util.List;

import com.xybakaqaq.mixin.ChatComponentAccessor;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.Chat;
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
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.TextColor;

/**
 * The Skija-rendered replacement for the vanilla chat message list (the {@code Chat} module):
 * the wrapped lines are taken from the vanilla {@link ChatComponent} (via {@link ChatComponentAccessor}),
 * drawn newest-at-bottom with per-run text colors, the vanilla time-based fade while unfocused, and a
 * translucent backdrop per row. Draggable while the chat bar is open, like the other HUD widgets.
 */
public final class ChatWidget {

	private static final String WINDOW = "chat";
	private static final int ROW_H = 10;
	private static final int PAD = 4;
	/** Widest text block, in pixels; the panel is this plus {@link #PAD} on both sides. */
	private static final int MAX_TEXT_W = 320;
	private static final int FADE_TICKS = 200;

	private static final DragState drag = new DragState(WINDOW);
	private static final SkijaBridge bridge = new SkijaBridge("hud_chat");
	private static final Font font = new Font(Skia.harmony(), 9.5f);

	/** A text run of one color. */
	private record Run(String text, int rgb) {
	}

	private ChatWidget() {
	}

	public static void render(GuiGraphicsExtractor graphics, boolean chatOpen) {
		Minecraft mc = Minecraft.getInstance();
		Chat module = ModuleManager.get(Chat.class);
		if (mc.player == null || module == null) {
			return;
		}
		ChatComponent chat = mc.gui.hud.getChat();
		List<GuiMessage.Line> lines = ((ChatComponentAccessor) chat).getTrimmedMessages();
		if (lines.isEmpty()) {
			return;
		}
		int scroll = ((ChatComponentAccessor) chat).getChatScrollbarPos();
		int now = mc.gui.hud.getGuiTicks();

		// collect rows newest-first (chat index 0 is the newest line), stopping at the page size or,
		// while unfocused, at the first fully faded line
		List<Run[]> rows = new ArrayList<>();
		List<Float> rowAlphas = new ArrayList<>();
		int perPage = chat.getLinesPerPage();
		for (int i = scroll; i < lines.size() && rows.size() < perPage; i++) {
			GuiMessage.Line line = lines.get(i);
			float alpha = chatOpen ? 1f : fade(now - line.addedTime());
			if (!chatOpen && alpha <= 0.01f) {
				break;
			}
			rows.add(runs(line));
			rowAlphas.add(alpha);
		}
		if (rows.isEmpty()) {
			return;
		}

		// The panel either hugs the longest visible line (adaptive) or keeps a fixed width, so the
		// backdrop stays put as messages scroll away.
		int textW = MAX_TEXT_W;
		if (module.adaptiveWidth()) {
			float maxW = 0f;
			for (Run[] row : rows) {
				float w = 0f;
				for (Run run : row) {
					w += font.measureTextWidth(run.text());
				}
				maxW = Math.max(maxW, w);
			}
			textW = Math.min(MAX_TEXT_W, (int) Math.ceil(maxW));
		}
		int panelW = textW + PAD * 2;
		int panelH = rows.size() * ROW_H;

		int guiW = graphics.guiWidth();
		int guiH = graphics.guiHeight();
		drag.update(chatOpen, panelW, panelH, guiW, guiH, 0, guiH - panelH - 40);
		int panelX = drag.x();
		int panelY = drag.y();

		int background = module.background();
		List<Object> parts = new ArrayList<>();
		parts.add(background);
		for (int i = 0; i < rows.size(); i++) {
			parts.add(Math.round(rowAlphas.get(i) * 32f));
			for (Run run : rows.get(i)) {
				parts.add(run.text());
				parts.add(run.rgb());
			}
		}
		long key = SkijaBridge.key(parts.toArray());

		Window win = mc.getWindow();
		float scale = win.getGuiScaledWidth() > 0 ? (float) win.getWidth() / win.getGuiScaledWidth() : 1f;
		int sw = Math.max(1, Math.round(panelW * scale));
		int sh = Math.max(1, Math.round(panelH * scale));
		try {
			bridge.render(sw, sh, key, canvas -> {
				canvas.scale(scale, scale);
				draw(canvas, rows, rowAlphas, panelW, background);
			});
			graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), panelX, panelY,
					0.0F, 0.0F, panelW, panelH, sw, sh, sw, sh);
		} catch (Throwable ignored) {
			// skip this frame
		}
	}

	private static void draw(Canvas canvas, List<Run[]> rows, List<Float> alphas, int panelW, int background) {
		FontMetrics m = font.getMetrics();
		int bgAlpha = background >>> 24;
		try (Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			for (int i = 0; i < rows.size(); i++) {
				// rows are newest-first; the newest line sits at the bottom of the panel
				float y = (rows.size() - 1 - i) * ROW_H;
				float alpha = alphas.get(i);
				paint.setColor((Math.round(alpha * bgAlpha) << 24) | (background & 0xFFFFFF));
				canvas.drawRect(Rect.makeXYWH(0, y, panelW, ROW_H), paint);
				float baseline = y + ROW_H / 2f - (m.getAscent() + m.getDescent()) / 2f;
				float x = PAD;
				int textAlpha = Math.round(alpha * 255) << 24;
				for (Run run : rows.get(i)) {
					paint.setColor(textAlpha | run.rgb());
					canvas.drawString(run.text(), x, baseline, font, paint);
					x += font.measureTextWidth(run.text());
				}
			}
		}
	}

	/** Vanilla's time-based fade: fully visible for 10 s, then a quadratic ease-out. */
	private static float fade(int tickDelta) {
		double t = 1.0 - tickDelta / (double) FADE_TICKS;
		t *= 10.0;
		t = Math.max(0.0, Math.min(1.0, t));
		return (float) (t * t);
	}

	/** Splits a line into same-color runs (Style color; default white). */
	private static Run[] runs(GuiMessage.Line line) {
		List<Run> out = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		int[] rgb = {0xFFFFFF};
		line.content().accept((position, style, codepoint) -> {
			TextColor color = style.getColor();
			int value = color == null ? 0xFFFFFF : color.getValue();
			if (sb.length() > 0 && value != rgb[0]) {
				out.add(new Run(sb.toString(), rgb[0]));
				sb.setLength(0);
			}
			rgb[0] = value;
			sb.appendCodePoint(codepoint);
			return true;
		});
		if (sb.length() > 0) {
			out.add(new Run(sb.toString(), rgb[0]));
		}
		return out.toArray(new Run[0]);
	}

	/** Begins a drag if the left button presses inside the panel (uses the last drawn rect). */
	public static boolean mouseClicked(double mx, double my, int button) {
		return drag.mouseClicked(mx, my, button);
	}
}
