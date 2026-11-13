package com.xybakaqaq.gui.mainmenu;

import java.util.ArrayList;
import java.util.List;

import com.xybakaqaq.config.UiConfig;
import com.xybakaqaq.utils.Sdf;
import com.xybakaqaq.utils.skia.Skia;
import com.xybakaqaq.utils.skia.SkijaBridge;

import com.mojang.blaze3d.platform.Window;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;

// Skija chrome for the world select and multiplayer pages, including our own layout: the list sits on
// the left, the actions stack down a right-hand column. Their own render pass is cancelled and redrawn
// here; the list and search box still come from their vanilla render states.
public final class MenuListPage {

	private static final int TEXT = 0xFFF2F2F4;
	private static final int SUBTEXT = 0xFF9A9AA2;
	private static final int DISABLED = 0xFF5A5A62;
	private static final int ACCENT = 0xFFC20C0C;

	private static final int HEADER = 42;
	private static final int SIDEBAR_INSET = 9;
	private static final int GAP = 12;

	private static final SkijaBridge cache = new SkijaBridge("menu_list_page");

	private MenuListPage() {
	}

	// True for the screens this page owns.
	public static boolean handles(Screen screen) {
		return UiConfig.customMenu()
				&& (screen instanceof SelectWorldScreen || screen instanceof JoinMultiplayerScreen);
	}

	// Moves the list, the search field and the buttons into our layout. Called after the vanilla page
	// has arranged its widgets, so it wins.
	public static void layout(Screen screen) {
		if (!handles(screen)) {
			return;
		}
		List<AbstractButton> buttons = collectButtons(screen);
		AbstractSelectionList<?> list = collectList(screen);
		EditBox search = collectSearch(screen);

		int w = screen.width;
		int h = screen.height;
		int margin = margin(w);
		int sidebarW = sidebarWidth(w);
		int available = Math.max(160, w - margin * 2);

		// Fill the window: the list takes everything left of the action column. Its rows are widened
		// to match the list (see WorldSelectionListMixin / ServerSelectionListMixin), so the entries
		// no longer float in the middle of an oversized panel.
		int listW = Math.max(140, available - GAP - sidebarW);
		int startX = margin;
		int sidebarX = startX + listW + GAP;
		int listY = HEADER;
		int contentH = contentHeight(h, margin);

		if (list != null) {
			// Note the parameter order: width, height, x, y.
			list.updateSizeAndPosition(listW, contentH, startX, listY);
		}
		// Search field sits in the header above the action column, with no frame of its own: only the
		// text plus an underline (see render()).
		if (search != null) {
			search.setX(sidebarX);
			search.setY(8);
			search.setWidth(sidebarW);
			search.setHeight(20);
			search.setBordered(false);
		}
		if (!buttons.isEmpty()) {
			int count = buttons.size();
			int buttonH = 22;
			int gap = 7;
			int room = contentH - SIDEBAR_INSET * 2;
			if (count * buttonH + (count - 1) * gap > room) {
				buttonH = Math.max(12, (room - (count - 1) * 3) / count);
				gap = Math.max(2, Math.min(7, (room - count * buttonH) / Math.max(1, count - 1)));
			}
			int buttonW = Math.max(48, sidebarW - SIDEBAR_INSET * 2);
			int x = sidebarX + (sidebarW - buttonW) / 2;
			int y = listY + SIDEBAR_INSET;
			for (AbstractButton button : buttons) {
				button.setWidth(buttonW);
				button.setHeight(buttonH);
				button.setX(x);
				button.setY(y);
				y += buttonH + gap;
			}
		}
	}

	public static void render(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		List<AbstractButton> buttons = collectButtons(screen);
		AbstractSelectionList<?> list = collectList(screen);
		EditBox search = collectSearch(screen);

		// The backdrop stays put (it has its own drifting glows); only the page content slides in.
		MenuBackdrop.draw(graphics);
		float open = MenuAnimation.open();
		boolean sliding = open < 0.999f;
		if (sliding) {
			graphics.pose().pushMatrix();
			graphics.pose().translate(0f, Math.round((1f - open) * 16f));
		}

		drawChrome(screen, graphics, buttons, list, search, open);
		drawHover(graphics, buttons, mouseX, mouseY);

		if (list != null) {
			list.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
		}
		// The search box keeps all of its vanilla input handling; only the pixels are ours.
		if (search != null) {
			MenuTextField.drawField(graphics, search);
		}
		if (sliding) {
			graphics.pose().popMatrix();
		}
	}

	/* ------------------------------------------------------------------ collection */

	private static List<AbstractButton> collectButtons(Screen screen) {
		List<AbstractButton> buttons = new ArrayList<>();
		for (GuiEventListener child : screen.children()) {
			if (child instanceof AbstractButton button && button.visible) {
				buttons.add(button);
			}
		}
		return buttons;
	}

	private static AbstractSelectionList<?> collectList(Screen screen) {
		for (GuiEventListener child : screen.children()) {
			if (child instanceof AbstractSelectionList<?> list) {
				return list;
			}
		}
		return null;
	}

	private static EditBox collectSearch(Screen screen) {
		for (GuiEventListener child : screen.children()) {
			if (child instanceof EditBox editBox) {
				return editBox;
			}
		}
		return null;
	}

	private static int margin(int width) {
		return Math.max(12, Math.min(26, Math.round(width * 0.022f)));
	}

	private static int sidebarWidth(int width) {
		return Math.max(96, Math.min(170, Math.round(width * 0.22f)));
	}

	private static int contentHeight(int height, int margin) {
		return Math.max(48, height - margin - HEADER);
	}

	/* ------------------------------------------------------------------ rendering */

	private static void drawChrome(Screen screen, GuiGraphicsExtractor graphics,
			List<AbstractButton> buttons, AbstractSelectionList<?> list, EditBox search, float open) {
		int guiW = graphics.guiWidth();
		int guiH = graphics.guiHeight();
		Window win = Minecraft.getInstance().getWindow();
		float renderScale = Math.min(2f, guiW > 0 ? (float) win.getWidth() / guiW : 1f);
		int sw = Math.max(1, Math.round(guiW * renderScale));
		int sh = Math.max(1, Math.round(guiH * renderScale));
		String title = screen.getTitle().getString();
		long key = key(sw, sh, title, buttons, list, search);

		try {
			cache.render(sw, sh, key, canvas -> {
				canvas.scale(renderScale, renderScale);
				drawBaked(canvas, guiW, guiH, title, buttons, list, search);
			});
		} catch (Throwable ignored) {
			return;
		}
		// Same texture, tinted by the open animation so the chrome fades in instead of popping.
		int alpha = Math.round(255f * Math.max(0f, Math.min(1f, open)));
		int tint = (alpha << 24) | 0xFFFFFF;
		graphics.blit(RenderPipelines.GUI_TEXTURED, cache.textureId(), 0, 0, 0.0F, 0.0F,
				guiW, guiH, sw, sh, sw, sh, tint);
	}

	private static long key(int sw, int sh, String title, List<AbstractButton> buttons,
			AbstractSelectionList<?> list, EditBox search) {
		List<Object> parts = new ArrayList<>();
		parts.add(sw);
		parts.add(sh);
		parts.add(title);
		if (list != null) {
			parts.add(list.getX());
			parts.add(list.getY());
			parts.add(list.getWidth());
			parts.add(list.getHeight());
		}
		if (search != null) {
			parts.add(search.getX());
			parts.add(search.getY());
			parts.add(search.getWidth());
		}
		for (AbstractButton button : buttons) {
			parts.add(button.getX());
			parts.add(button.getY());
			parts.add(button.getWidth());
			parts.add(button.getHeight());
			parts.add(button.getMessage().getString());
			parts.add(button.active);
		}
		return SkijaBridge.key(parts.toArray());
	}

	private static void drawBaked(Canvas canvas, int w, int h, String title,
			List<AbstractButton> buttons, AbstractSelectionList<?> list, EditBox search) {
		int margin = margin(w);
		int sidebarW = sidebarWidth(w);
		// Both the title and the action column hang off the list's real position, so nothing drifts
		// out of line when the list is resized.
		int startX = list != null ? list.getX() : margin;
		int sidebarX = list != null ? list.getX() + list.getWidth() + GAP : w - margin - sidebarW;
		int contentH = contentHeight(h, margin);

		// Header: title on the left, above the list.
		try (Font font = new Font(Skia.harmonyMedium(), 11f); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			float right = search != null ? search.getX() - 8f : w - margin;
			float room = Math.max(8f, right - startX - 4f);
			canvas.save();
			canvas.clipRect(Rect.makeXYWH(startX, 0, Math.max(20f, room + 4f), 14));
			paint.setColor(TEXT);
			canvas.drawString(title, startX, 9.5f, font, paint);
			paint.setColor(ACCENT);
			canvas.drawRRect(RRect.makeXYWH(startX + 1, 11.5f, Math.min(font.measureTextWidth(title), room), 1.5f, 1), paint);
			canvas.restore();
		}

		try (Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			// Panels: the list on the left, the action column on the right (both replace the vanilla
			// list background, which is suppressed while the page is themed).
			if (list != null) {
				paint.setColor(0x4D141420);
				canvas.drawRRect(RRect.makeXYWH(list.getX(), list.getY(),
						list.getWidth(), list.getHeight(), 6), paint);
			}
			paint.setColor(0x3A141420);
			canvas.drawRRect(RRect.makeXYWH(sidebarX, HEADER, sidebarW, contentH, 6), paint);
			paint.setColor(0x66C20C0C);
			canvas.drawRRect(RRect.makeXYWH(sidebarX + 14, HEADER, Math.max(1, sidebarW - 28), 1.5f, 1), paint);
		}

		// Action column: the first button is the primary action and gets the accent face.
		float labelSize = Math.max(9f, Math.min(12f, h / 34f));
		try (Font font = new Font(Skia.harmony(), labelSize); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			FontMetrics metrics = font.getMetrics();
			for (int i = 0; i < buttons.size(); i++) {
				AbstractButton button = buttons.get(i);
				int bx = button.getX();
				int by = button.getY();
				int bw = button.getWidth();
				int bh = button.getHeight();
				boolean primary = i == 0 && button.active;
				paint.setColor(primary ? 0x59C20C0C : (button.active ? 0x66141420 : 0x33141420));
				canvas.drawRRect(RRect.makeXYWH(bx, by, bw, bh, 5), paint);
				String label = button.getMessage().getString();
				float baseline = by + bh / 2f - (metrics.getAscent() + metrics.getDescent()) / 2f;
				paint.setColor(button.active ? (primary ? 0xFFF4E8E8 : SUBTEXT) : DISABLED);
				canvas.save();
				canvas.clipRect(Rect.makeXYWH(bx + 4, by, Math.max(1, bw - 8), bh));
				canvas.drawString(label, bx + (bw - font.measureTextWidth(label)) / 2f, baseline, font, paint);
				canvas.restore();
			}
		}
	}

	private static void drawHover(GuiGraphicsExtractor graphics, List<AbstractButton> buttons, int mouseX, int mouseY) {
		for (AbstractButton button : buttons) {
			if (!button.active) {
				continue;
			}
			int bx = button.getX();
			int by = button.getY();
			int bw = button.getWidth();
			int bh = button.getHeight();
			boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
			float hv = MenuAnimation.hover(button, hovered);
			if (hv > 0.01f) {
				Sdf.roundRect(graphics, bx, by, bw, bh, 5, alpha(0x40FFFFFF, hv));
				Sdf.roundRect(graphics, bx, by, 3, bh, 3, alpha(ACCENT, hv));
			}
		}
	}

	private static int alpha(int color, float factor) {
		int a = Math.round((color >>> 24) * Math.max(0f, Math.min(1f, factor)));
		return (a << 24) | (color & 0xFFFFFF);
	}
}
