package com.xybakaqaq.gui.mainmenu;

import com.xybakaqaq.config.UiConfig;
import com.xybakaqaq.mixin.WinScreenAccessor;
import com.xybakaqaq.utils.Sdf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import net.minecraft.client.gui.screens.CreateFlatWorldScreen;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.GenericWaitingScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.PresetFlatWorldScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.RecoverWorldDataScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.WorldOptionsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

// Generic Skija skin for the menu pages we keep vanilla: creates/edits worlds, options, packs,
// server screens, confirmations and the loading screens. Those pages still render themselves; we only
// replace the background, the list panels and the faces of their buttons, sliders and labels.
public final class MenuTheme {

	private static final int TEXT = 0xFFE8E8EA;
	private static final int DISABLED = 0xFF5A5A62;
	private static final int ACCENT = 0xFFC20C0C;

	private MenuTheme() {
	}

	public static boolean themed(Screen screen) {
		if (screen == null || !UiConfig.customMenu()) {
			return false;
		}
		if (screen instanceof LevelLoadingScreen
				|| screen instanceof SelectWorldScreen || screen instanceof JoinMultiplayerScreen) {
			return true;
		}
		String pkg = screen.getClass().getPackageName();
		if (pkg.equals("net.minecraft.client.gui.screens.worldselection")
				|| pkg.startsWith("net.minecraft.client.gui.screens.options")
				|| pkg.startsWith("net.minecraft.client.gui.screens.packs")) {
			return true;
		}
		// Second-level pages reached from those (world options/customisation, server management,
		// confirmations, join warnings). Only outside a world, so in-game dialogs keep the blurred
		// game behind them.
		if (Minecraft.getInstance().level != null) {
			return false;
		}
		if (pkg.equals("net.minecraft.client.gui.screens.multiplayer")) {
			return true;
		}
		return screen instanceof ConfirmScreen || screen instanceof AlertScreen
				|| screen instanceof WorldOptionsScreen || screen instanceof CreateFlatWorldScreen
				|| screen instanceof PresetFlatWorldScreen || screen instanceof CreateBuffetWorldScreen
				|| screen instanceof DirectJoinServerScreen || screen instanceof ManageServerScreen
				|| screen instanceof NoticeWithLinkScreen || screen instanceof DisconnectedScreen
				|| screen instanceof ProgressScreen || screen instanceof ConnectScreen
				|| screen instanceof RecoverWorldDataScreen || screen instanceof DatapackLoadFailureScreen
				|| screen instanceof CreditsAndAttributionScreen || screen instanceof BackupConfirmScreen
				|| screen instanceof GenericMessageScreen || screen instanceof GenericWaitingScreen
				// Only the credits roll; the end poem keeps its end-portal sky.
				|| (screen instanceof WinScreen win && !((WinScreenAccessor) win).zeroday$poem())
				|| screen instanceof AccessibilityOnboardingScreen;
	}

	// The two list pages draw their own chrome.
	public static boolean bakesChrome(Screen screen) {
		return themed(screen) && !(screen instanceof SelectWorldScreen) && !(screen instanceof JoinMultiplayerScreen);
	}

	public static boolean suppressListBackground() {
		return themed(Minecraft.getInstance().gui.screen());
	}

	// Plain text buttons only: no sprites, no blanks, no tabs.
	public static boolean skinnable(AbstractButton button) {
		if (button.getMessage().getString().isBlank()) {
			return false;
		}
		String name = button.getClass().getName();
		if (name.contains("TabButton") || name.contains("SpriteIconButton") || name.contains("Checkbox")) {
			return false;
		}
		return button.getWidth() > 16 && button.getHeight() > 8;
	}

	// Backdrop plus a rounded panel under every list, then the vanilla background is cancelled.
	public static void renderBackground(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		MenuLabel.beginFrame(screen);
		MenuBackdrop.draw(graphics);
		for (GuiEventListener child : screen.children()) {
			if (child instanceof AbstractSelectionList<?> list && list.visible) {
				Sdf.roundRect(graphics, list.getX(), list.getY(), list.getWidth(), list.getHeight(), 6, 0x4D141420);
			}
		}
	}

	// Button face: rounded panel, accent bar on hover, label on top. Drawn live by the widget so it also
	// covers the buttons nested inside option lists.
	public static void renderButton(GuiGraphicsExtractor graphics, AbstractButton button, int mouseX, int mouseY) {
		int x = button.getX();
		int y = button.getY();
		int w = button.getWidth();
		int h = button.getHeight();
		boolean active = button.active;
		boolean hovered = active && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		float hv = MenuAnimation.hover(button, hovered);

		Sdf.roundRect(graphics, x, y, w, h, 5, active ? 0x66141420 : 0x33141420);
		if (hv > 0.01f) {
			Sdf.roundRect(graphics, x, y, w, h, 5, alpha(0x2EFFFFFF, hv));
			Sdf.roundRect(graphics, x, y, Math.max(1, Math.round(3 * hv)), h, 3, alpha(ACCENT, hv));
		}
		MenuLabel.drawCentered(graphics, button, button.getMessage().getString(), active ? TEXT : DISABLED);
	}

	// Panel, thin progress bar along the bottom, round handle, label on top. Drawn live because the
	// label carries the value and changes while dragging.
	public static void renderSlider(GuiGraphicsExtractor graphics, AbstractSliderButton slider, double value,
			int mouseX, int mouseY) {
		int x = slider.getX();
		int y = slider.getY();
		int w = slider.getWidth();
		int h = slider.getHeight();
		boolean active = slider.active;
		boolean hovered = active && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;

		Sdf.roundRect(graphics, x, y, w, h, 4, active ? 0x66141420 : 0x33141420);
		if (hovered) {
			Sdf.roundRect(graphics, x, y, w, h, 4, 0x18FFFFFF);
		}

		int inset = 8;
		int trackW = Math.max(1, w - inset * 2);
		int barY = y + h - 5;
		float v = (float) Math.max(0d, Math.min(1d, value));

		Sdf.roundRect(graphics, x + inset, barY, trackW, 2, 1, 0x2EFFFFFF);
		int fillW = Math.round(trackW * v);
		if (fillW > 0) {
			Sdf.roundRect(graphics, x + inset, barY, fillW, 2, 1, 0xE6C20C0C);
		}

		int knobX = x + inset + fillW;
		Sdf.roundRect(graphics, knobX - 3, barY - 2, 6, 6, 3, active ? 0xFFFFFFFF : 0xFF8A8A92);

		MenuLabel.drawCentered(graphics, slider, slider.getMessage().getString(), active ? 0xFFE8E8EA : 0xFF8A8A92);
	}

	// Label drawn in place of a vanilla text widget (titles and option list headers).
	public static void renderText(GuiGraphicsExtractor graphics, AbstractWidget widget, String text, float inset) {
		float size = Math.max(7.5f, Math.min(9f, widget.getHeight()));
		MenuLabel.draw(graphics, widget, text, TEXT, size, 0f, inset);
	}

	private static int alpha(int color, float factor) {
		int a = Math.round((color >>> 24) * Math.max(0f, Math.min(1f, factor)));
		return (a << 24) | (color & 0xFFFFFF);
	}
}
