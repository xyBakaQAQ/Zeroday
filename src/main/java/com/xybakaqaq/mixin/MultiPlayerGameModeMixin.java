package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.targethud.TargetHudWidget;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feeds the player's attack target to the {@link TargetHudWidget}. */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

	@Inject(method = "attack", at = @At("HEAD"))
	private void zeroday$onAttack(Player player, Entity entity, CallbackInfo ci) {
		TargetHudWidget.onAttack(entity);
	}
}
