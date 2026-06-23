package com.xybakaqaq.modules.render;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.utils.InventoryUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Visual inventory helper (ported from Flowing, visual mode only): tints inventory / container slots
 * green when the item is better than what you already have (or the best of its kind you own) and red
 * when it is junk. No automation — purely the highlight.
 */
public final class InventoryHelper extends Module {

	private static final int USEFUL = 0x5000FF00;
	private static final int GARBAGE = 0x50FF0000;
	private static final double EPS = 0.0001D;

	private final BooleanSetting checkTools = new BooleanSetting("Check Tools", true);

	private final Set<Integer> usefulSlots = new HashSet<>();
	private final Set<Integer> garbageSlots = new HashSet<>();
	private long lastUpdate;

	public InventoryHelper() {
		super("InventoryHelper", Category.RENDER, "Highlights better (green) / junk (red) inventory slots.", GLFW.GLFW_KEY_UNKNOWN);
	}

	/** @return the highlight ARGB for the slot, or 0. Called from the container-screen mixin per slot. */
	public int getSlotHighlightColor(Slot slot) {
		if (!isEnabled()) {
			return 0;
		}
		refresh();
		if (usefulSlots.contains(slot.index)) {
			return USEFUL;
		}
		if (garbageSlots.contains(slot.index)) {
			return GARBAGE;
		}
		return 0;
	}

	private void refresh() {
		long now = System.currentTimeMillis();
		if (now - lastUpdate < 60L) {
			return;
		}
		lastUpdate = now;
		usefulSlots.clear();
		garbageSlots.clear();
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.gui.screen() instanceof AbstractContainerScreen<?> screen) {
			updateHelpers(mc.player, screen.getMenu().slots);
		}
	}

	private void updateHelpers(Player player, List<Slot> slots) {
		if (player.getAbilities().instabuild || player.isSpectator()) {
			return;
		}
		for (Slot slot : slots) {
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			String category = InventoryUtils.getHelperItemCategory(stack);
			if (!checkTools.isEnabled() && InventoryUtils.isMiningToolCategory(category)) {
				continue;
			}
			ItemLevel level = getItemLevel(player, stack, slot);
			if (level == ItemLevel.BETTER) {
				usefulSlots.add(slot.index);
			} else if (level == ItemLevel.GARBAGE) {
				garbageSlots.add(slot.index);
			}
		}
	}

	private ItemLevel getItemLevel(Player player, ItemStack stack, Slot slot) {
		String category = InventoryUtils.getHelperItemCategory(stack);
		if (category.equals("other") && !InventoryUtils.isCleanerWhitelisted(stack.getItem())) {
			return ItemLevel.GARBAGE;
		}
		if (!category.startsWith("tool_") && !category.startsWith("weapon_") && !category.startsWith("armor_")) {
			return ItemLevel.NORMAL;
		}
		double currentScore = InventoryUtils.calculateScore(player, stack);
		double playerMax = InventoryUtils.getPlayerMaxScore(player, category);
		if (slot.container == player.getInventory()) {
			int bestSlot = InventoryUtils.getBestPlayerInventorySlot(player, category);
			if (slot.getContainerSlot() == bestSlot && currentScore > 0.0D && Math.abs(currentScore - playerMax) < EPS) {
				return ItemLevel.BETTER;
			}
			return ItemLevel.GARBAGE;
		}
		return currentScore > playerMax + EPS ? ItemLevel.BETTER : ItemLevel.GARBAGE;
	}

	private enum ItemLevel {
		BETTER, NORMAL, GARBAGE
	}
}
