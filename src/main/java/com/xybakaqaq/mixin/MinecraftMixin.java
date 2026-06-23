package com.xybakaqaq.mixin;

import com.xybakaqaq.event.EventManager;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link EventManager#onTick} at the start of every client tick so modules can run
 * per-tick logic.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

	@Inject(method = "tick", at = @At("HEAD"))
	private void zeroday$onTick(CallbackInfo ci) {
		EventManager.onTick();
	}
}
