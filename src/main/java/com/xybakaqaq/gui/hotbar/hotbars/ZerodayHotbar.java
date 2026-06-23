package com.xybakaqaq.gui.hotbar.hotbars;

import com.xybakaqaq.gui.hotbar.Hotbar;
import com.xybakaqaq.gui.hotbar.HotbarManager;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.HUD;
import com.xybakaqaq.utils.HotbarLayout;
import com.xybakaqaq.utils.ItemTooltipUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Zeroday-style hotbar: a frameless, magnified "dock" of item icons floating above the
 * health bar. The selected slot is enlarged (with nearby slots scaling down smoothly);
 * empty slots show their number 1-9. Item count/durability decorations are kept.
 *
 * <p>Layout and easing live in {@link HotbarLayout}; this class only does the drawing.
 * Drawn through {@link HotbarManager#onExtractHotbar} via the {@code Hud} mixin; returns
 * {@code true} so the vanilla hotbar is cancelled.
 */
public class ZerodayHotbar extends Hotbar {

	private static final int ICON = 16;
	private static final int GAP_ABOVE_HEALTH = 4; // px between item row and the health bar
	private static final int COL_EMPTY_NUMBER = 0xFFFFFFFF;

	@Override
	public boolean extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!isHotbarEnabled()) {
			return false; // Hotbar mode "off" — let vanilla render
		}

		Player player = mc.player;
		if (player == null) {
			return false; // no player yet — let vanilla handle it
		}

		Inventory inventory = player.getInventory();
		int selected = inventory.getSelectedSlot();
		float focus = HotbarLayout.animatedFocus(selected, deltaTracker);

		int centerX = graphics.guiWidth() / 2;
		int bottomY = baselineBottom(graphics, player); // bottom edge of the (unscaled) icons

		// Main 9 slots.
		for (int slot = 0; slot < HotbarLayout.SLOT_COUNT; slot++) {
			float scale = HotbarLayout.animatedScale(focus, slot, deltaTracker);
			int slotCenterX = HotbarLayout.slotCenterX(centerX, slot);
			drawSlot(graphics, player, inventory.getItem(slot), slot, slotCenterX, bottomY, scale);
		}

		// Offhand, just outside the row on the off-arm side.
		ItemStack offhand = player.getOffhandItem();
		if (!offhand.isEmpty()) {
			boolean left = player.getMainArm().getOpposite() == HumanoidArm.LEFT;
			int offCenterX = HotbarLayout.slotCenterX(centerX, left ? -1 : HotbarLayout.SLOT_COUNT);
			drawSlot(graphics, player, offhand, -1, offCenterX, bottomY, 1f);
		}

		return true; // vanilla hotbar replaced
	}

	/**
	 * Draws the held-item name above the selected slot (following it as it slides, fading with
	 * the highlight timer) plus a colored stat line below it — the Zeroday tooltip.
	 */
	@Override
	public boolean extractSelectedItemName(GuiGraphicsExtractor graphics, ItemStack stack, int timer) {
		if (!isHotbarEnabled()) {
			return false; // Hotbar mode "off" — let vanilla render the item name
		}
		if (stack.isEmpty() || timer <= 0) {
			return true; // we own this render; nothing to draw right now
		}
		int alpha = Math.min(255, (int) (timer * 256.0f / 10.0f));
		if (alpha <= 0) {
			return true;
		}

		Player player = mc.player;
		if (player == null) {
			return true;
		}

		MutableComponent name = Component.empty().append(stack.getHoverName()).withStyle(stack.getRarity().color());
		if (stack.has(DataComponents.CUSTOM_NAME)) {
			name.withStyle(ChatFormatting.ITALIC);
		}

		int centerX = graphics.guiWidth() / 2;
		// Left edge of the selected icon — both lines are left-aligned here and slide with the slot.
		int leftX = HotbarLayout.slotCenterX(centerX, HotbarLayout.focus()) - ICON / 2;
		int hotbarTop = baselineBottom(graphics, player) - Math.round(ICON * HotbarLayout.maxScale());
		int detailY = hotbarTop - 12;
		int nameY = detailY - 11;

		// Stat line (just above the hotbar), left-aligned.
		ItemTooltipUtil.StatLine stat = ItemTooltipUtil.statLine(stack);
		if (stat != null) {
			int color = (alpha << 24) | (stat.rgb() & 0xFFFFFF);
			graphics.text(mc.font, stat.text(), leftX, detailY, color);
		}

		// Item name (above the stat line), left-aligned, scaled up 1.2x about its left edge.
		float scale = 1.2f;
		graphics.pose().pushMatrix();
		graphics.pose().translate(leftX, nameY);
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(-leftX, -nameY);
		graphics.text(mc.font, name, leftX, nameY, (alpha << 24) | 0xFFFFFF);
		graphics.pose().popMatrix();
		return true;
	}

	/** @return whether the Zeroday hotbar style is active (HUD module's Hotbar mode = zeroday). */
	private static boolean isHotbarEnabled() {
		HUD hud = ModuleManager.get(HUD.class);
		return hud == null || hud.isHotbarZeroday();
	}

	/**
	 * Draws one slot, scaled about its bottom-centre so enlarged icons grow upward and stay
	 * horizontally centred. {@code slot} is the inventory index (1-9 label = slot + 1); pass
	 * a negative slot for the offhand (no empty-number fallback).
	 */
	private void drawSlot(GuiGraphicsExtractor graphics, Player player,
			ItemStack stack, int slot, int centerX, int bottomY, float scale) {
		int itemX = centerX - ICON / 2;
		int itemY = bottomY - ICON;

		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX, bottomY);
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(-centerX, -bottomY);

		if (!stack.isEmpty()) {
			graphics.item(player, stack, itemX, itemY, slot + 1);
			graphics.itemDecorations(mc.font, stack, itemX, itemY);
		} else if (slot >= 0) {
			String label = String.valueOf(slot + 1);
			int textX = centerX - mc.font.width(label) / 2;
			int textY = itemY + (ICON - mc.font.lineHeight) / 2;
			graphics.text(mc.font, label, textX, textY, COL_EMPTY_NUMBER);
		}

		graphics.pose().popMatrix();
	}

	/** Y of the icon bottom edge: a few px above the health bar, raised for extra heart rows. */
	private static int baselineBottom(GuiGraphicsExtractor graphics, Player player) {
		int healthLineTop = graphics.guiHeight() - 39; // vanilla health row top
		int extraRows = heartRows(player) - 1;
		return healthLineTop - extraRows * 9 - GAP_ABOVE_HEALTH;
	}

	/** Number of heart rows the vanilla health bar would occupy (10 hearts / 20 hp per row). */
	private static int heartRows(Player player) {
		float hp = Math.max(player.getMaxHealth(), player.getHealth() + player.getAbsorptionAmount());
		int hearts = (int) Math.ceil(hp / 2.0);
		return Math.max(1, (int) Math.ceil(hearts / 10.0));
	}
}
