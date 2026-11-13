package com.xybakaqaq.utils;

import java.util.Locale;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

// Computes the stat line shown under the held-item name:
// <ul>
// <li>food → a plain "Foods" label</li>
// <li>mining tools (pickaxe / shovel / hoe) → a plain "Tools" label</li>
// <li>weapons and axes → attack damage</li>
// </ul>
// All lines are white. Armor is not shown. Axes are treated as weapons so they show attack
// damage rather than the "Tools" label.
public final class ItemTooltipUtil {

	// A ready-to-draw stat line plus its base RGB colour (alpha applied by the caller).
	public record StatLine(String text, int rgb) {
	}

	private static final int WHITE = 0xFFFFFF;

	private ItemTooltipUtil() {
	}

	// Stat line for the held item, or null if it has no notable stat.
	public static StatLine statLine(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		if (stack.has(DataComponents.FOOD)) {
			return new StatLine("Foods", WHITE);
		}
		if (isMiningTool(stack)) {
			return new StatLine("Tools", WHITE);
		}
		double bonus = InventoryUtils.getAttributeValue(stack, Attributes.ATTACK_DAMAGE);
		if (bonus != 0.0) {
			return new StatLine("+" + fmt(attackDamage(stack, bonus)) + " Attack Damage", WHITE);
		}
		return null;
	}

	// True for pickaxes, shovels and hoes. 26.3's tools are data-driven, so they're identified by
	// their item tags; axes are not in this set and fall through to the attack-damage line.
	private static boolean isMiningTool(ItemStack stack) {
		Holder<Item> item = stack.typeHolder();
		return item.is(ItemTags.PICKAXES) || item.is(ItemTags.SHOVELS) || item.is(ItemTags.HOES);
	}

	private static double attackDamage(ItemStack stack, double bonus) {
		double base = 1.0 + bonus;
		int sharpness = enchantLevel(stack, Enchantments.SHARPNESS);
		return base + (sharpness > 0 ? sharpness * 0.5 + 0.5 : 0.0);
	}

	private static int enchantLevel(ItemStack stack, ResourceKey<Enchantment> key) {
		ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
		if (enchantments == null) {
			return 0;
		}
		for (Holder<Enchantment> holder : enchantments.keySet()) {
			if (holder.is(key)) {
				return enchantments.getLevel(holder);
			}
		}
		return 0;
	}

	private static String fmt(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}
}
