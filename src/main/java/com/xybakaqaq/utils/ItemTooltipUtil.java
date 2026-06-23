package com.xybakaqaq.utils;

import java.util.Locale;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;

/**
 * Computes the stat line shown under the held-item name:
 * <ul>
 *   <li>food → a plain "Foods" label</li>
 *   <li>mining tools (pickaxe / shovel / hoe) → a plain "Tools" label</li>
 *   <li>weapons and axes → attack damage</li>
 * </ul>
 *
 * All lines are white. Armor is not shown. Axes ({@code AxeItem} still exists in 26.2) are
 * treated as weapons so they show attack damage rather than the "Tools" label.
 */
@SuppressWarnings("deprecation") // built-in registry / block-state accessors carry Mojang's soft-deprecation
public final class ItemTooltipUtil {

	/** A ready-to-draw stat line plus its base RGB colour (alpha applied by the caller). */
	public record StatLine(String text, int rgb) {
	}

	private static final int WHITE = 0xFFFFFF;

	private ItemTooltipUtil() {
	}

	/** Stat line for the held item, or {@code null} if it has no notable stat. */
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
		if (attributeAmount(stack, Attributes.ATTACK_DAMAGE) != 0) {
			return new StatLine("+" + fmt(attackDamage(stack)) + " Attack Damage", WHITE);
		}
		return null;
	}

	/**
	 * True for pickaxes, shovels and hoes. Shovels and hoes have dedicated classes; pickaxes
	 * don't, so they're detected by mining stone faster than their own default speed. Swords
	 * are efficient on leaves/plants (so leaves can't be used to detect tools) but never on
	 * stone, so they correctly fall through. Axes are treated as weapons and excluded.
	 */
	private static boolean isMiningTool(ItemStack stack) {
		Item item = stack.getItem();
		if (item instanceof AxeItem) {
			return false;
		}
		if (item instanceof HoeItem || item instanceof ShovelItem) {
			return true;
		}
		Tool tool = stack.get(DataComponents.TOOL);
		return tool != null && tool.getMiningSpeed(Blocks.STONE.defaultBlockState()) > tool.defaultMiningSpeed();
	}

	private static double attackDamage(ItemStack stack) {
		double base = 1.0 + attributeAmount(stack, Attributes.ATTACK_DAMAGE);
		int sharpness = enchantLevel(stack, Enchantments.SHARPNESS);
		return base + (sharpness > 0 ? sharpness * 0.5 + 0.5 : 0.0);
	}

	private static double attributeAmount(ItemStack stack, Holder<Attribute> attribute) {
		ItemAttributeModifiers mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
		if (mods == null) {
			return 0.0;
		}
		for (ItemAttributeModifiers.Entry entry : mods.modifiers()) {
			if (entry.attribute().is(attribute)) {
				return entry.modifier().amount();
			}
		}
		return 0.0;
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
