package com.xybakaqaq.gui.hotbar.hotbars;

import com.xybakaqaq.gui.hotbar.Hotbar;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.HUD;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// Hanabi-style hotbar, ported from Hanabi 3.1's HUD "Hotbar" element: a flat, unframed strip pinned
// to the bottom edge — 181x20, rgb(17,17,17) at ~78% alpha — nine 16px icons at the classic 20px
// pitch, the slot number drawn in place of an empty slot, and a 21x2 accent bar sitting on the
// bottom edge under the selected slot, sliding between slots with a 200 ms ease-out. Icons keep the
// vanilla selection pop. The strip sits exactly where the vanilla hotbar was, so the status bars
// above it stay put (see HudLayout). The offhand is drawn just outside the strip on the off-arm
// side: Hanabi 3.1 ran on 1.8.9 and had no offhand slot, modern Minecraft does.
// Drawn through HotbarManager#onExtractHotbar via the Hud mixin; returns true so the vanilla
// hotbar is cancelled.
public class HanabiHotbar extends Hotbar {

	private static final int SLOT_COUNT = 9;
	private static final int SLOT_PITCH = 20;
	private static final int ICON = 16;

	// Bar geometry, in scaled-GUI pixels, matching Hanabi's drawRect calls.
	private static final int BAR_W = 181;
	private static final int BAR_H = 20;
	// Distance from the bottom edge to the top of the bar (and to the slide underline).
	private static final int BAR_TOP = 22;
	private static final int UNDERLINE_W = 21;
	private static final int UNDERLINE_H = 2;
	// Gap between the strip and the offhand icon.
	private static final int OFFHAND_GAP = 13;

	// Hanabi: new Color(17, 17, 17, 200).
	private static final int BAR_COLOR = 0xC8111111;
	private static final int ACCENT = 0xFFC20C0C;
	private static final int SLOT_NUMBER = 0xFFFFFFFF;

	private static final long SLIDE_NANOS = 200_000_000L;

	// Underline slide state (one hotbar instance, so plain fields are fine).
	private float slideFrom;
	private float slideTo;
	private long slideStart;
	private boolean sliding;

	@Override
	public boolean extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!isHanabiEnabled()) {
			return false; // Hotbar mode is not "hanabi" — let vanilla render
		}
		Player player = mc.player;
		if (player == null) {
			return false; // no player yet — let vanilla handle it
		}

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		int left = width / 2 - 91;
		int iconY = height - BAR_TOP + 2;

		Inventory inventory = player.getInventory();
		int selected = inventory.getSelectedSlot();

		// Strip, then the underline under the selected slot (it slides with the selection).
		graphics.fill(left, height - BAR_TOP, left + BAR_W, height - BAR_TOP + BAR_H, BAR_COLOR);
		updateSlide(selected);
		int underlineX = left + Math.round(slide());
		graphics.fill(underlineX, height - UNDERLINE_H, underlineX + UNDERLINE_W, height, ACCENT);

		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);

		// The nine hotbar slots; empty ones show their number instead of an icon.
		for (int slot = 0; slot < SLOT_COUNT; slot++) {
			int x = width / 2 - 90 + slot * SLOT_PITCH + 2;
			ItemStack stack = inventory.getItem(slot);
			if (stack.isEmpty()) {
				String label = String.valueOf(slot + 1);
				int numberY = iconY + (ICON - mc.font.lineHeight) / 2;
				graphics.text(mc.font, label, x + 4, numberY, SLOT_NUMBER);
			} else {
				drawIcon(graphics, player, stack, x, iconY, slot + 1, partialTick);
			}
		}

		// Offhand, just outside the strip on the off-arm side.
		ItemStack offhand = player.getOffhandItem();
		if (!offhand.isEmpty()) {
			boolean offLeft = player.getMainArm().getOpposite() == HumanoidArm.LEFT;
			int offX = offLeft ? left - OFFHAND_GAP - ICON : left + BAR_W + OFFHAND_GAP;
			drawIcon(graphics, player, offhand, offX, iconY, 0, partialTick);
		}

		return true; // vanilla hotbar replaced
	}

	private static boolean isHanabiEnabled() {
		HUD hud = ModuleManager.get(HUD.class);
		return hud != null && hud.isHotbarHanabi();
	}

	// One icon plus its count/durability decorations, with the vanilla selection pop: right after a
	// slot is picked the icon squashes wide and stretches vertically, then settles.
	private void drawIcon(GuiGraphicsExtractor graphics, Player player, ItemStack stack, int x, int y,
			int seed, float partialTick) {
		float pop = stack.getPopTime() - partialTick;
		boolean bouncing = pop > 0f;
		if (bouncing) {
			float scale = 1f + pop / 5f;
			graphics.pose().pushMatrix();
			graphics.pose().translate(x + 8f, y + 12f);
			graphics.pose().scale(1f / scale, (scale + 1f) / 2f);
			graphics.pose().translate(-(x + 8f), -(y + 12f));
		}
		graphics.item(player, stack, x, y, seed);
		if (bouncing) {
			graphics.pose().popMatrix();
		}
		graphics.itemDecorations(mc.font, stack, x, y);
	}

	// Starts a slide when the selected slot changes; an in-flight slide keeps its target so quick
	// slot scrolling eases from the current position instead of snapping.
	private void updateSlide(int selected) {
		float target = selected * SLOT_PITCH;
		if (!sliding || target != slideTo) {
			slideFrom = slide();
			slideTo = target;
			slideStart = System.nanoTime();
			sliding = true;
		}
	}

	// Current underline offset in pixels, easing out over SLIDE_NANOS.
	private float slide() {
		if (!sliding) {
			return slideTo;
		}
		float t = (System.nanoTime() - slideStart) / (float) SLIDE_NANOS;
		if (t >= 1f) {
			sliding = false;
			return slideTo;
		}
		float remaining = 1f - t;
		return slideFrom + (slideTo - slideFrom) * (1f - remaining * remaining);
	}
}
