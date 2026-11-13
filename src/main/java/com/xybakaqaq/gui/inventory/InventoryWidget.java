package com.xybakaqaq.gui.inventory;

import com.xybakaqaq.utils.DragState;
import com.xybakaqaq.utils.skia.SkijaBridge;

import com.mojang.blaze3d.platform.Window;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.ItemStack;

// The Skija-rendered inventory overlay: the player's main storage (slots 9–35, the 9×3 grid above the
// hotbar — the hotbar itself is excluded). The cell grid is drawn with Skija; the item models + counts
// are rendered by the game on top. Draggable while the chat bar is open; position persisted.
public final class InventoryWidget {

	private static final String WINDOW = "inventory";
	private static final int FIRST_SLOT = 9;
	private static final int COLS = 9;
	private static final int ROWS = 3;
	private static final int SLOT = 18;
	private static final int CELL = 16;
	private static final int PAD = 3;
	private static final int PANEL_W = PAD * 2 + (COLS - 1) * SLOT + CELL;
	private static final int PANEL_H = PAD * 2 + (ROWS - 1) * SLOT + CELL;

	private static final DragState drag = new DragState(WINDOW);
	private static final SkijaBridge bridge = new SkijaBridge("hud_inventory");

	private InventoryWidget() {
	}

	public static void render(GuiGraphicsExtractor graphics, boolean chatOpen) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			return;
		}
		int guiW = graphics.guiWidth();
		int guiH = graphics.guiHeight();
		drag.update(chatOpen, PANEL_W, PANEL_H, guiW, guiH, 4, (guiH - PANEL_H) / 2);
		int x = drag.x();
		int y = drag.y();

		Window win = mc.getWindow();
		float scale = win.getGuiScaledWidth() > 0 ? (float) win.getWidth() / win.getGuiScaledWidth() : 1f;
		int sw = Math.max(1, Math.round(PANEL_W * scale));
		int sh = Math.max(1, Math.round(PANEL_H * scale));
		try {
			// the grid panel is static — render once, then just re-blit (items are drawn separately)
			bridge.render(sw, sh, 1L, canvas -> {
				canvas.scale(scale, scale);
				drawPanel(canvas);
			});
			graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), x, y, 0.0F, 0.0F,
					PANEL_W, PANEL_H, sw, sh, sw, sh);
		} catch (Throwable ignored) {
			return;
		}

		// item models + counts rendered by the game on top of the Skija grid
		for (int i = 0; i < COLS * ROWS; i++) {
			ItemStack stack = mc.player.getInventory().getItem(FIRST_SLOT + i);
			if (stack.isEmpty()) {
				continue;
			}
			int ix = x + PAD + (i % COLS) * SLOT;
			int iy = y + PAD + (i / COLS) * SLOT;
			graphics.item(stack, ix, iy);
			graphics.itemDecorations(mc.font, stack, ix, iy);
		}
	}

	private static void drawPanel(Canvas canvas) {
		try (Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			paint.setColor(0xC01A1A20);
			canvas.drawRRect(RRect.makeXYWH(0, 0, PANEL_W, PANEL_H, 5), paint);
			paint.setColor(0x40000000);
			for (int i = 0; i < COLS * ROWS; i++) {
				float cx = PAD + (i % COLS) * SLOT;
				float cy = PAD + (i / COLS) * SLOT;
				canvas.drawRRect(RRect.makeXYWH(cx, cy, CELL, CELL, 2), paint);
			}
		}
	}

	// Begins a drag if the left button presses inside the panel.
	public static boolean mouseClicked(double mx, double my, int button) {
		return drag.mouseClicked(mx, my, button);
	}
}
