package com.xybakaqaq.gui.cloudmusic;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Window;

import com.xybakaqaq.config.WindowConfig;
import com.xybakaqaq.utils.Sdf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * The small persistent now-playing widget drawn in the HUD while the CloudMusic module is enabled:
 * cover thumbnail, title, artist and a progress bar. When the chat bar is open the mouse is free, so
 * a row of playback controls (previous / play-pause / next) appears below it. While chat is open you
 * can <b>drag the widget body with the left button</b> to move it, and <b>right-click</b> it to open
 * the full {@link CloudMusicScreen}. Clicks/drags are routed here from the chat screen.
 */
public final class CloudMusicWidget {

	private static final int COVER = 34;
	private static final int PAD = 6;
	private static final int GAP = 8;
	private static final int W = 196;
	private static final int H = COVER + PAD * 2;

	private static final int BTN_H = 18;
	private static final int BTN_GAP = 4;
	private static final int BTN_W = (W - BTN_GAP * 2) / 3;

	private static final int TEXT = 0xFFE8E8EA;
	private static final int SUBTEXT = 0xFF9A9AA2;
	private static final int ACCENT = 0xFFC20C0C;
	private static final int LYRIC = 0xFFD8A8A8;

	private static final String WINDOW = "musicplayer";

	// Mutable position (draggable), persisted to windows.json.
	private static int x = 6;
	private static int y = 6;
	private static boolean posLoaded;
	private static boolean dragging;
	private static double dragOffX;
	private static double dragOffY;

	private CloudMusicWidget() {
	}

	public static void render(GuiGraphicsExtractor graphics, Font font, boolean chatOpen) {
		if (!posLoaded) {
			x = WindowConfig.getX(WINDOW, x);
			y = WindowConfig.getY(WINDOW, y);
			posLoaded = true;
		}
		if (!chatOpen) {
			dragging = false;
		} else if (dragging) {
			updateDrag();
		}

		Sdf.roundRect(graphics, x, y, W, H, 8, 0xE01D1D24, 0xE013131A);

		int cx = x + PAD;
		int cy = y + PAD;
		if (NowPlaying.hasCover) {
			Sdf.roundRect(graphics, cx, cy, COVER, COVER, 5, 0x40000000);
			graphics.blit(RenderPipelines.GUI_TEXTURED, NowPlaying.COVER, cx, cy, 0.0F, 0.0F,
					COVER, COVER, NowPlaying.COVER_PX, NowPlaying.COVER_PX, NowPlaying.COVER_PX, NowPlaying.COVER_PX);
		} else {
			Sdf.roundRect(graphics, cx, cy, COVER, COVER, 5, 0x33FFFFFF);
		}

		int tx = cx + COVER + GAP;
		int tw = x + W - PAD - tx;
		graphics.text(font, trim(font, NowPlaying.title, tw), tx, cy + 1, TEXT);
		graphics.text(font, trim(font, NowPlaying.artist, tw), tx, cy + 11, SUBTEXT);
		String lyric = NowPlaying.currentLyric();
		if (!lyric.isEmpty()) {
			graphics.text(font, trim(font, lyric, tw), tx, cy + 21, LYRIC);
		}

		int by = cy + COVER - 3;
		long dur = NowPlaying.durationMs;
		float frac = dur > 0 ? Math.min(1f, MusicPlayer.positionMs() / (float) dur) : 0f;
		Sdf.roundRect(graphics, tx, by, tw, 3, 1, 0x33FFFFFF);
		if (frac > 0) {
			Sdf.roundRect(graphics, tx, by, Math.max(2, (int) (tw * frac)), 3, 1, ACCENT);
		}

		if (chatOpen) {
			boolean playing = MusicPlayer.isActive() && !MusicPlayer.isPaused();
			drawButton(graphics, font, 0, "|<");
			drawButton(graphics, font, 1, playing ? "||" : ">");
			drawButton(graphics, font, 2, ">|");
		}
	}

	/**
	 * Handles a press while the chat bar is open: right-click opens the full UI; left-click on a
	 * control button drives playback; left-click on the body begins a drag.
	 *
	 * @return whether the press was consumed by the widget.
	 */
	public static boolean mouseClicked(double mx, double my, int button) {
		if (button == 1 && in(mx, my, x, y, W, H)) {
			Minecraft.getInstance().gui.setScreen(new CloudMusicScreen());
			return true;
		}
		if (button == 0) {
			if (in(mx, my, btnX(0), btnY(), BTN_W, BTN_H)) {
				NowPlaying.previous();
				return true;
			}
			if (in(mx, my, btnX(1), btnY(), BTN_W, BTN_H)) {
				MusicPlayer.togglePause();
				return true;
			}
			if (in(mx, my, btnX(2), btnY(), BTN_W, BTN_H)) {
				NowPlaying.next();
				return true;
			}
			if (in(mx, my, x, y, W, H)) {
				dragging = true;
				dragOffX = mx - x;
				dragOffY = my - y;
				return true;
			}
		}
		return false;
	}

	/** Polls the live cursor + left-button state to follow a drag (no mouseDragged event needed). */
	private static void updateDrag() {
		Minecraft mc = Minecraft.getInstance();
		Window win = mc.getWindow();
		if (GLFW.glfwGetMouseButton(win.handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
			dragging = false;
			WindowConfig.set(WINDOW, x, y);
			return;
		}
		double mx = mc.mouseHandler.xpos() * win.getGuiScaledWidth() / win.getScreenWidth();
		double my = mc.mouseHandler.ypos() * win.getGuiScaledHeight() / win.getScreenHeight();
		x = clamp((int) Math.round(mx - dragOffX), 0, win.getGuiScaledWidth() - W);
		y = clamp((int) Math.round(my - dragOffY), 0, win.getGuiScaledHeight() - H);
	}

	private static void drawButton(GuiGraphicsExtractor graphics, Font font, int idx, String label) {
		int bx = btnX(idx);
		int byy = btnY();
		Sdf.roundRect(graphics, bx, byy, BTN_W, BTN_H, 5, 0xE0262633, 0xE01A1A22);
		graphics.centeredText(font, Component.literal(label), bx + BTN_W / 2, byy + 5, TEXT);
	}

	private static int btnX(int idx) {
		return x + idx * (BTN_W + BTN_GAP);
	}

	private static int btnY() {
		return y + H + 4;
	}

	private static boolean in(double mx, double my, int rx, int ry, int rw, int rh) {
		return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
	}

	private static int clamp(int v, int lo, int hi) {
		return Math.max(lo, Math.min(v, hi));
	}

	private static String trim(Font font, String s, int maxWidth) {
		if (s == null) {
			return "";
		}
		if (font.width(s) <= maxWidth) {
			return s;
		}
		while (s.length() > 1 && font.width(s + "…") > maxWidth) {
			s = s.substring(0, s.length() - 1);
		}
		return s + "…";
	}
}
