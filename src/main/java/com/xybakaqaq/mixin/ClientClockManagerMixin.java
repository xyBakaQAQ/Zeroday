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

// Reports the Environment time tick for the overworld clock, so the timeline-sampled sun
// angle, sky color and brightness all follow the chosen time of day.
// 26.3 removed ClientClockManager.getTotalTicks(Holder); every consumer (timeline
// samplers, Level.getClockTimeTicks, commands) now goes through
// clockManager.getInstance(clock).totalTicks(). Handing out a stub instance whose
// totalTicks() is the chosen time therefore covers all of them.
@Mixin(ClientClockManager.class)
public class ClientClockManagerMixin {

	@Inject(method = "getInstance", at = @At("HEAD"), cancellable = true)
	private void zeroday$overrideTime(Holder<WorldClock> definition,
			CallbackInfoReturnable<ClientClockManager.ClientClockInstance> cir) {
		if (Environment.timeActive() && definition.is(WorldClocks.OVERWORLD)) {
			cir.setReturnValue(OverriddenClockInstance.INSTANCE);
		}
	}

	// Stand-in clock whose tick count is the module's chosen time of day. Lives outside the mixin
	// body so no extra field or method is merged into ClientClockManager.
	static final class OverriddenClockInstance extends ClientClockManager.ClientClockInstance {

		static final OverriddenClockInstance INSTANCE = new OverriddenClockInstance();

		private OverriddenClockInstance() {
		}

		@Override
		public long totalTicks() {
			return Environment.ticks();
		}

		@Override
		public float partialTick() {
			return 0.0F;
		}

		@Override
		public float rate() {
			return 1.0F;
		}

		@Override
		public boolean isPaused() {
			return false;
		}
	}
}
