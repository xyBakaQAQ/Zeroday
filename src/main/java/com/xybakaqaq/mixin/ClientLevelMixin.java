package com.xybakaqaq.mixin;

import com.xybakaqaq.modules.render.Environment;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Forces the rain/snow precipitation type for the {@link Environment} weather override. */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

	@Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
	private void zeroday$precipitation(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
		Biome.Precipitation forced = Environment.precipType();
		if (forced != null) {
			cir.setReturnValue(forced);
		}
	}
}
