package com.xybakaqaq.mixin;

import com.xybakaqaq.modules.render.Environment;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reports the {@link Environment} time tick for the overworld clock, so the timeline-sampled sun
 * angle, sky color and brightness all follow the chosen time of day.
 */
@Mixin(ClientClockManager.class)
public class ClientClockManagerMixin {

	@Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
	private void zeroday$overrideTime(Holder<WorldClock> definition, CallbackInfoReturnable<Long> cir) {
		if (Environment.timeActive() && definition.is(WorldClocks.OVERWORLD)) {
			cir.setReturnValue(Environment.ticks());
		}
	}
}
