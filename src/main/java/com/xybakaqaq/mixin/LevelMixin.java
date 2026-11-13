package com.xybakaqaq.mixin;

import com.xybakaqaq.modules.render.Environment;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Reports the Environment rain / thunder levels for the client level only (guarded by
// this == mc.level), so the integrated server's weather logic stays untouched.
@Mixin(Level.class)
public class LevelMixin {

	@Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
	private void zeroday$rainLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (Environment.weatherActive() && (Object) this == Minecraft.getInstance().level) {
			cir.setReturnValue(Environment.rainLevel());
		}
	}

	@Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
	private void zeroday$thunderLevel(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (Environment.weatherActive() && (Object) this == Minecraft.getInstance().level) {
			cir.setReturnValue(Environment.thunderLevel());
		}
	}
}
