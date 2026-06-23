package com.xybakaqaq.mixin;

import java.util.Locale;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.Animation;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Legacy first-person combat animations for the {@link Animation} module: while holding right-click
 * with a sword/axe, the held item is drawn in an old-style block pose (Sigma / Old / Astra / Slide /
 * Swank), offset by the module's Item X/Y/Z. When not blocking, vanilla rendering is left untouched.
 * Swords have no vanilla use action, so the blocking intent is detected via {@link Animation#blocking()}.
 *
 * <p>{@code applyItemArmTransform} = Yarn {@code applyEquipOffset}; {@code applyItemArmAttackTransform}
 * = Yarn {@code applySwingOffset}.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

	@Shadow
	private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight) {
		throw new AssertionError();
	}

	@Shadow
	private void applyItemArmAttackTransform(PoseStack poseStack, HumanoidArm arm, float attackValue) {
		throw new AssertionError();
	}

	@Shadow
	public void renderItem(LivingEntity mob, ItemStack itemStack, ItemDisplayContext type, PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector, int lightCoords) {
		throw new AssertionError();
	}

	@Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
	private void zeroday$legacyCombat(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
			float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
		Animation animation = ModuleManager.get(Animation.class);
		if (animation == null || !animation.isEnabled()) {
			return;
		}
		if (hand != InteractionHand.MAIN_HAND || player.isScoping() || !animation.isWeapon(itemStack)) {
			return;
		}
		if (!Animation.blocking()) {
			return;
		}

		HumanoidArm arm = player.getMainArm();
		boolean right = arm == HumanoidArm.RIGHT;
		float side = right ? 1.0F : -1.0F;

		poseStack.pushPose();
		poseStack.translate(animation.itemX() * side, animation.itemY(), animation.itemZ());
		applyMode(poseStack, animation.mode().toLowerCase(Locale.ROOT), arm, attack, inverseArmHeight, player);
		this.renderItem(player, itemStack,
				right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
				poseStack, submitNodeCollector, lightCoords);
		poseStack.popPose();
		ci.cancel();
	}

	private void applyMode(PoseStack poseStack, String mode, HumanoidArm arm, float attack, float equipProgress, AbstractClientPlayer player) {
		float pi = (float) Math.PI;
		float f1 = Mth.sin(Mth.sqrt(attack) * pi);
		switch (mode) {
			case "old" -> {
				poseStack.translate(-0.2F * f1, 0.0F, -0.1F * Mth.sin(attack * pi));
				this.applyItemArmTransform(poseStack, arm, 0.0F);
				this.applyItemArmAttackTransform(poseStack, arm, attack);
				baseRotation(poseStack);
			}
			case "astra" -> {
				poseStack.translate(-0.05F, 0.08F, 0.0F);
				this.applyItemArmTransform(poseStack, arm, equipProgress / 1.42F);
				this.applyItemArmAttackTransform(poseStack, arm, attack);
				poseStack.mulPose(Axis.YP.rotationDegrees(-f1 * -30.0F));
				poseStack.mulPose(Axis.YP.rotationDegrees(-f1 * 60.0F));
				poseStack.mulPose(Axis.ZP.rotationDegrees(-f1 * 32.0F));
				baseRotation(poseStack);
			}
			case "slide" -> {
				poseStack.translate(-0.05F, 0.08F, 0.0F);
				this.applyItemArmTransform(poseStack, arm, equipProgress / 1.42F);
				this.applyItemArmAttackTransform(poseStack, arm, attack);
				poseStack.mulPose(Axis.YP.rotationDegrees(-f1 * -30.0F));
				baseRotation(poseStack);
			}
			case "swank" -> {
				poseStack.translate(-0.05F, 0.15F, 0.0F);
				this.applyItemArmTransform(poseStack, arm, equipProgress / 1.42F);
				this.applyItemArmAttackTransform(poseStack, arm, attack);
				float swankS = Mth.sin(Mth.sqrt(attack));
				poseStack.mulPose(Axis.YP.rotationDegrees(-swankS * -1.0F));
				poseStack.mulPose(Axis.YP.rotationDegrees(77.0F));
				poseStack.mulPose(Axis.ZP.rotationDegrees(-2.0F));
				poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
			}
			case "sigma" -> {
				float sigmaS = Mth.sin(Mth.sqrt(attack) * pi);
				poseStack.translate(-0.05F, player.isShiftKeyDown() ? 0.08F : 0.15F, -0.1F);
				this.applyItemArmTransform(poseStack, arm, 0.0F);
				this.applyItemArmAttackTransform(poseStack, arm, attack);
				poseStack.mulPose(Axis.YP.rotationDegrees(sigmaS * 30.0F));
				poseStack.mulPose(Axis.XP.rotationDegrees(-sigmaS * 40.0F));
				baseRotation(poseStack);
			}
			default -> {
				this.applyItemArmTransform(poseStack, arm, 0.0F);
				this.applyItemArmAttackTransform(poseStack, arm, attack);
				baseRotation(poseStack);
			}
		}
	}

	/** Shared finishing rotation used by most block modes. */
	private void baseRotation(PoseStack poseStack) {
		poseStack.mulPose(Axis.YP.rotationDegrees(77.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F));
		poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
	}
}
