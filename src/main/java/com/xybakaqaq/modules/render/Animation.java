package com.xybakaqaq.modules.render;

import org.lwjgl.glfw.GLFW;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.modules.settings.ModeSetting;
import com.xybakaqaq.modules.settings.NumberSetting;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Legacy first-person combat animations: holding right-click with a sword (or axe, optionally) shows
 * an old-style block pose. The {@code Mode} setting picks the style — Sigma / Old / Astra / Slide /
 * Swank — handled in {@link com.xybakaqaq.mixin.ItemInHandRendererMixin}. Looking at an interactive
 * block (chest, door, etc.) suppresses the pose so you can still use it.
 */
public final class Animation extends Module {

	private final ModeSetting mode = new ModeSetting("Mode", "Sigma", "Sigma", "Old", "Astra", "Slide", "Swank");
	private final BooleanSetting axes = new BooleanSetting("Axes", false);
	private final NumberSetting itemX = new NumberSetting("Item X", 0.0, -1.0, 1.0, 0.01);
	private final NumberSetting itemY = new NumberSetting("Item Y", 0.0, -1.0, 1.0, 0.01);
	private final NumberSetting itemZ = new NumberSetting("Item Z", 0.0, -1.0, 1.0, 0.01);

	public Animation() {
		super("Animation", Category.RENDER, "Legacy sword block animations (right-click). Multiple modes.", GLFW.GLFW_KEY_UNKNOWN);
	}

	public String mode() {
		return mode.getValue();
	}

	public float itemX() {
		return itemX.getValue().floatValue();
	}

	public float itemY() {
		return itemY.getValue().floatValue();
	}

	public float itemZ() {
		return itemZ.getValue().floatValue();
	}

	/** @return whether {@code stack} is a weapon the block pose applies to (sword, or axe when enabled). */
	public boolean isWeapon(ItemStack stack) {
		return stack.is(ItemTags.SWORDS) || (axes.isEnabled() && stack.is(ItemTags.AXES));
	}

	/**
	 * @return whether the block pose should currently show: right-click held and not aiming at an
	 *         interactive block (so opening chests / doors still works).
	 */
	public static boolean blocking() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || !mc.options.keyUse.isDown()) {
			return false;
		}
		HitResult hit = mc.hitResult;
		if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
			Block block = mc.level.getBlockState(((BlockHitResult) hit).getBlockPos()).getBlock();
			if (isInteractive(block)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isInteractive(Block block) {
		return block instanceof ChestBlock || block instanceof EnderChestBlock
				|| block instanceof TrapDoorBlock || block instanceof DoorBlock
				|| block instanceof AnvilBlock || block instanceof LeverBlock
				|| block instanceof ButtonBlock || block instanceof FenceGateBlock;
	}
}
