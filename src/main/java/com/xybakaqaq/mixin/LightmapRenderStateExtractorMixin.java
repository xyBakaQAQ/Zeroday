package com.xybakaqaq.mixin;

import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.FullBright;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// FullBright: while the module is enabled, replaces the gamma value the lightmap reads
// (options.gamma().get().floatValue() in extract) with a high value so the world
// is fully lit. The first Double.floatValue() in extract is the gamma read.
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {

	@Redirect(
			method = "extract",
			at = @At(value = "INVOKE", target = "Ljava/lang/Double;floatValue()F", ordinal = 0)
	)
	private float zeroday$fullBright(Double gamma) {
		FullBright module = ModuleManager.get(FullBright.class);
		if (module != null && module.isEnabled()) {
			return 15.0f; // gamma well above the vanilla 0–1 range → fully lit
		}
		return gamma.floatValue();
	}
}
