package com.xybakaqaq.mixin;

import com.xybakaqaq.utils.Sdf;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Keeps a second copy of the scissor stack (the vanilla one is private) so the SDF panels can be
// clipped to the active scissor exactly like vanilla blits.
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {

	@Inject(method = "enableScissor", at = @At("HEAD"))
	private void zeroday$pushScissor(int x1, int y1, int x2, int y2, CallbackInfo ci) {
		GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) (Object) this;
		Sdf.pushScissor(new ScreenRectangle(x1, y1, x2 - x1, y2 - y1)
				.transformAxisAligned(graphics.pose()));
	}

	@Inject(method = "disableScissor", at = @At("HEAD"))
	private void zeroday$popScissor(CallbackInfo ci) {
		Sdf.popScissor();
	}
}
