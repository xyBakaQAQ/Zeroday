package com.xybakaqaq.modules.player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.utils.InventoryUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Visual inventory helper (ported from Flowing, visual mode only): compares only weapons and
// armor against the best of their kind you own — the backpack, hotbar, offhand AND the armor
// you are wearing, so chest contents are always measured against your equipped gear too. Your own
// slots are tinted green when (tied for) the best — including identical duplicates and worn gear —
// and red when strictly worse. In chests, items strictly better than your best are green, strictly
// worse are red, and ties are left untinted. Everything else (tools, blocks, food, misc) is left
// untinted. No automation.
public final class InventoryHelper extends Module {

	private static final int USEFUL = 0x5000FF00;
	private static final int GARBAGE = 0x50FF0000;
	private static final double EPS = 0.0001D;

	private final Set<Integer> usefulSlots = new HashSet<>();
	private final Set<Integer> garbageSlots = new HashSet<>();
	private long lastUpdate;

	public InventoryHelper() {
		super("InventoryHelper", Category.PLAYER, "Highlights better (green) / worse (red) weapon & armor slots.", InputConstants.UNKNOWN.getValue());
	}


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
		// per-refresh cache of each category's best owned score (armor scoring walks enchantments,
		// so don't recompute it per slot)
		Map<String, Double> bestOwned = new HashMap<>();
		for (Slot slot : slots) {
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			ItemLevel level = getItemLevel(player, stack, slot, bestOwned);
			if (level == ItemLevel.BETTER) {
				usefulSlots.add(slot.index);
			} else if (level == ItemLevel.GARBAGE) {
				garbageSlots.add(slot.index);
			}
		}
	}

	private ItemLevel getItemLevel(Player player, ItemStack stack, Slot slot, Map<String, Double> bestOwned) {
		String category = InventoryUtils.getHelperItemCategory(stack);
		// only weapons and armor are compared — everything else is left untinted
		if (!category.startsWith("weapon_") && !category.startsWith("armor_")) {
			return ItemLevel.NORMAL;
		}
		// the best of its kind among everything you own: backpack, hotbar, offhand AND the armor
		// you are wearing — so chest contents are always measured against your equipped gear too
		double best = bestOwned.computeIfAbsent(category, c -> InventoryUtils.getPlayerMaxScore(player, c));
		double score = InventoryUtils.calculateScore(player, stack);
		if (slot.container == player.getInventory()) {
			// tied for the best of its kind (incl. identical duplicates and the gear you're
			// wearing) → green; strictly worse than something else you own → red
			return score > 0.0D && score >= best - EPS ? ItemLevel.BETTER : ItemLevel.GARBAGE;
		}
		// chest etc.: strictly better than your best → green, strictly worse → red, tie → untinted
		if (score > best + EPS) {
			return ItemLevel.BETTER;
		}
		return score < best - EPS ? ItemLevel.GARBAGE : ItemLevel.NORMAL;
	}

	private enum ItemLevel {
		BETTER, NORMAL, GARBAGE
	}
}
