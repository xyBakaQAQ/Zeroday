package com.xybakaqaq.utils;

import com.xybakaqaq.config.WindowConfig;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Minecraft;
import com.xybakaqaq.utils.IMinecraft;

/**
 * Shared drag-and-position state for the on-screen HUD widgets (ArrayList, Potions, Inventory,
 * Keystrokes, TargetHUD). Each widget owns one static instance keyed by its {@code windows.json}
 * name; the panel is top-left anchored and draggable while the chat bar is open.
 *
 * <p>The position is kept as a fraction (0..1) of the free travel range (gui size minus panel
 * size), so a widget placed at, say, 80% across the screen stays at 80% when the gui scale or
 * window size changes. The fraction persists to {@code windows.json} as {@code "fx"}/{@code "fy"};
 * legacy absolute {@code "x"}/{@code "y"} entries are converted on load.
 *
 * <p>Usage: call {@link #update} once per frame from the widget's render (it lazily loads the
 * persisted position, advances an active drag, clamps the panel on-screen and records the drawn
 * rect), and route chat-screen clicks to {@link #mouseClicked}.
 */
public final class DragState {

	/** SDL mouse button indices, as reported by 26.3's {@code MouseButtonEvent.button()}. */
	private static final int SDL_BUTTON_LEFT = 1;

	private final String window;

	// position as a fraction (0..1) of the free travel range
	private double fx;
	private double fy;
	private boolean posLoaded;
	private boolean dragging;
	private double dragOffX;
	private double dragOffY;

	// panel's top-left corner + last drawn rect, for rendering and click hit-testing
	private int x;
	private int y;
	private int lastX;
	private int lastY;
	private int lastW;
	private int lastH;

	public DragState(String window) {
		this.window = window;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	/**
	 * Per-frame housekeeping: loads the persisted position on first call (falling back to
	 * {@code defX}/{@code defY}), updates the drag while the chat bar is open, converts the
	 * fractional position to pixels for the current gui size and remembers the drawn rect for
	 * hit-testing.
	 */
	public void update(boolean chatOpen, int panelW, int panelH, int guiW, int guiH, int defX, int defY) {
		int rangeX = Math.max(0, guiW - panelW);
		int rangeY = Math.max(0, guiH - panelH);
		if (!posLoaded) {
			fx = WindowConfig.getFraction(window, "fx", WindowConfig.getX(window, defX) / (double) Math.max(1, rangeX));
			fy = WindowConfig.getFraction(window, "fy", WindowConfig.getY(window, defY) / (double) Math.max(1, rangeY));
			posLoaded = true;
		}
		if (!chatOpen) {
			dragging = false;
		} else if (dragging) {
			updateDrag(rangeX, rangeY);
		}
		fx = clamp(fx, 0.0, 1.0);
		fy = clamp(fy, 0.0, 1.0);
		x = (int) Math.round(fx * rangeX);
		y = (int) Math.round(fy * rangeY);
		lastX = x;
		lastY = y;
		lastW = panelW;
		lastH = panelH;
	}

	/** Begins a drag if the left button (SDL button 1) presses inside the last drawn rect. */
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == SDL_BUTTON_LEFT && mx >= lastX && mx < lastX + lastW && my >= lastY && my < lastY + lastH) {
			dragging = true;
			dragOffX = mx - x;
			dragOffY = my - y;
			return true;
		}
		return false;
	}

	private void updateDrag(int rangeX, int rangeY) {
		Minecraft mc = Minecraft.getInstance();
		Window win = mc.getWindow();
		if (!mc.mouseHandler.isLeftPressed()) {
			dragging = false;
			WindowConfig.setFraction(window, fx, fy);
			return;
		}
		double mx = mc.mouseHandler.xpos() * win.getGuiScaledWidth() / win.getScreenWidth();
		double my = mc.mouseHandler.ypos() * win.getGuiScaledHeight() / win.getScreenHeight();
		fx = (mx - dragOffX) / Math.max(1, rangeX);
		fy = (my - dragOffY) / Math.max(1, rangeY);
	}

	private static double clamp(double v, double lo, double hi) {
		return Math.max(lo, Math.min(v, hi));
	}
}
