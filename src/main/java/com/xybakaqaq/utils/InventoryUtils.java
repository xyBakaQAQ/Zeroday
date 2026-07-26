package com.xybakaqaq.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;

/**
 * Item scoring / categorisation for the visual {@code InventoryHelper}. Ported from Flowing
 * (Yarn 1.21.4) to the Mojang-mapped 26.2 APIs, keeping only what the visual highlight needs.
 */
public final class InventoryUtils {

	private static final Set<Item> CLEANER_WHITELIST = new HashSet<>(Arrays.asList(
			Items.ENDER_PEARL, Items.ENDER_EYE, Items.TRIDENT, Items.MACE, Items.TNT,
			Items.BOW, Items.CROSSBOW, Items.ARROW,
			Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.APPLE,
			Items.MUSHROOM_STEW, Items.BREAD, Items.PORKCHOP, Items.COOKED_PORKCHOP,
			Items.GOLDEN_CARROT, Items.CARROT, Items.POTATO, Items.BAKED_POTATO,
			Items.COOKED_BEEF, Items.BEEF, Items.COOKED_CHICKEN, Items.CHICKEN,
			Items.COOKED_MUTTON, Items.MUTTON, Items.COOKED_RABBIT, Items.RABBIT,
			Items.RABBIT_STEW, Items.BEETROOT, Items.BEETROOT_SOUP, Items.MELON_SLICE,
			Items.PUMPKIN_PIE, Items.COOKIE, Items.SWEET_BERRIES, Items.COD,
			Items.COOKED_COD, Items.SALMON, Items.COOKED_SALMON, Items.HONEY_BOTTLE,
			Items.DRIED_KELP, Items.COMPASS, Items.RECOVERY_COMPASS,
			Items.WATER_BUCKET, Items.ELYTRA, Items.FLINT_AND_STEEL,
			Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND,
			Items.EMERALD, Items.NETHERITE_INGOT));

	private static final Set<Item> BLOCK_BLACKLIST = new HashSet<>(Arrays.asList(
			Items.TNT, Items.SAND, Items.RED_SAND, Items.GRAVEL,
			Items.CHEST, Items.TRAPPED_CHEST, Items.ENDER_CHEST, Items.BARREL,
			Items.HOPPER, Items.DISPENSER, Items.DROPPER,
			Items.CRAFTING_TABLE, Items.FURNACE, Items.BLAST_FURNACE, Items.SMOKER,
			Items.ENCHANTING_TABLE, Items.BREWING_STAND,
			Items.GRINDSTONE, Items.SMITHING_TABLE, Items.CARTOGRAPHY_TABLE,
			Items.FLETCHING_TABLE, Items.LOOM, Items.STONECUTTER,
			Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL, Items.DRAGON_EGG,
			Items.SUSPICIOUS_SAND, Items.SUSPICIOUS_GRAVEL));

	private InventoryUtils() {
	}

	public static String getItemCategory(ItemStack stack) {
		Item item = stack.getItem();
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (isArmor(stack) && equippable != null) {
			return "armor_" + equippable.slot().getName();
		}
		if (stack.is(ItemTags.SWORDS)) {
			return "weapon_sword";
		}
		if (item instanceof BowItem) {
			return "weapon_bow";
		}
		if (item instanceof CrossbowItem) {
			return "weapon_crossbow";
		}
		if (stack.is(ItemTags.PICKAXES)) {
			return "tool_pickaxe";
		}
		if (stack.is(ItemTags.SHOVELS)) {
			return "tool_shovel";
		}
		if (stack.is(ItemTags.AXES)) {
			return "tool_axe";
		}
		if (stack.is(ItemTags.HOES)) {
			return "tool_hoe";
		}
		if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) {
			return "golden_apple";
		}
		if (item == Items.ENDER_PEARL) {
			return "pearl";
		}
		if (stack.has(DataComponents.FOOD)) {
			return "food";
		}
		if (item instanceof BlockItem) {
			return "block";
		}
		if (item == Items.ARROW || item == Items.SPECTRAL_ARROW || item == Items.TIPPED_ARROW) {
			return "arrow";
		}
		return "other";
	}

	public static String getHelperItemCategory(ItemStack stack) {
		Item item = stack.getItem();
		String category = getItemCategory(stack);
		if (item instanceof BlockItem && BLOCK_BLACKLIST.contains(item)) {
			category = "other";
		}
		if ("block".equals(category)) {
			return "block";
		}
		if (!"other".equals(category)) {
			return category;
		}
		if (item == Items.SNOWBALL || item == Items.EGG || item == Items.EXPERIENCE_BOTTLE || item == Items.WIND_CHARGE) {
			return "throwable";
		}
		if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
			return "potion";
		}
		if (item instanceof SpawnEggItem || isCleanerWhitelisted(item)) {
			return "valuable";
		}
		return "other";
	}

	public static boolean isMiningToolCategory(String category) {
		return "tool_pickaxe".equals(category) || "tool_shovel".equals(category)
				|| "tool_axe".equals(category) || "tool_hoe".equals(category);
	}

	public static boolean isArmor(ItemStack stack) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable == null || !isArmorSlot(equippable.slot())) {
			return false;
		}
		return getAttributeValue(stack, Attributes.ARMOR) > 0.0D
				|| getAttributeValue(stack, Attributes.ARMOR_TOUGHNESS) > 0.0D;
	}

	public static boolean isArmorSlot(EquipmentSlot slot) {
		return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
				|| slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
	}

	public static boolean isCleanerWhitelisted(Item item) {
		return CLEANER_WHITELIST.contains(item);
	}

	/**
	 * @return the best score of the given category among everything the player owns: the backpack,
	 *         hotbar, offhand and the armor being worn ({@code -1} if they own nothing of it).
	 */
	public static double getPlayerMaxScore(Player player, String category) {
		double max = -1.0D;
		for (ItemStack stack : playerGear(player)) {
			if (!stack.isEmpty() && category.equals(getHelperItemCategory(stack))) {
				max = Math.max(max, calculateScore(player, stack));
			}
		}
		return max;
	}

	public static double calculateScore(Player player, ItemStack stack) {
		if (stack.isEmpty()) {
			return 0.0D;
		}
		if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) {
			return getAttributeValue(stack, Attributes.ATTACK_DAMAGE)
					+ getEnchantmentLevel(player, stack, Enchantments.SHARPNESS);
		}
		if (stack.has(DataComponents.TOOL)) {
			double baseSpeed = getMaxMiningSpeed(stack);
			int efficiency = getEnchantmentLevel(player, stack, Enchantments.EFFICIENCY);
			return baseSpeed + (efficiency > 0 ? (efficiency * efficiency) + 1.0D : 0.0D);
		}
		if (isArmor(stack)) {
			return calculateArmorScore(player, stack);
		}
		if (stack.getItem() instanceof BowItem) {
			return getEnchantmentLevel(player, stack, Enchantments.POWER) * 2.0D
					+ getEnchantmentLevel(player, stack, Enchantments.PUNCH);
		}
		return 0.0D;
	}

	public static double calculateArmorScore(Player player, ItemStack stack) {
		if (!isArmor(stack)) {
			return 0.0D;
		}
		double armor = getAttributeValue(stack, Attributes.ARMOR);
		double toughness = getAttributeValue(stack, Attributes.ARMOR_TOUGHNESS);
		int protection = getEnchantmentLevel(player, stack, Enchantments.PROTECTION);
		double[] testDamages = {4.0D, 8.0D, 12.0D, 16.0D, 20.0D};
		double totalReduction = 0.0D;
		for (double damage : testDamages) {
			totalReduction += damageReduction(armor, toughness, damage);
		}
		double averageReduction = totalReduction / testDamages.length;
		return Math.min(0.80D, averageReduction + protection * 0.04D) * 100.0D;
	}

	public static int getEnchantmentLevel(Player player, ItemStack stack, ResourceKey<Enchantment> key) {
		Holder<Enchantment> enchantment = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
		return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
	}

	public static double getAttributeValue(ItemStack stack, Holder<Attribute> attribute) {
		ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		double total = 0.0D;
		for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
			if (entry.attribute().equals(attribute)) {
				total += entry.modifier().amount();
			}
		}
		return total;
	}

	public static float getMaxMiningSpeed(ItemStack stack) {
		Tool tool = stack.get(DataComponents.TOOL);
		if (tool == null) {
			return 1.0F;
		}
		float maxSpeed = tool.defaultMiningSpeed();
		for (Tool.Rule rule : tool.rules()) {
			if (rule.speed().isPresent()) {
				maxSpeed = Math.max(maxSpeed, rule.speed().get());
			}
		}
		return maxSpeed;
	}

	private static List<ItemStack> playerGear(Player player) {
		List<ItemStack> list = new ArrayList<>(player.getInventory().getNonEquipmentItems());
		list.add(player.getItemBySlot(EquipmentSlot.HEAD));
		list.add(player.getItemBySlot(EquipmentSlot.CHEST));
		list.add(player.getItemBySlot(EquipmentSlot.LEGS));
		list.add(player.getItemBySlot(EquipmentSlot.FEET));
		list.add(player.getItemBySlot(EquipmentSlot.OFFHAND));
		return list;
	}

	private static double damageReduction(double armor, double toughness, double damage) {
		double effectiveArmor = Math.max(0.2D * armor, armor - damage / 2.0D + 0.25D * toughness);
		effectiveArmor = Math.min(20.0D, effectiveArmor);
		return Math.min(0.80D, Math.max(0.0D, effectiveArmor / 25.0D));
	}
}
