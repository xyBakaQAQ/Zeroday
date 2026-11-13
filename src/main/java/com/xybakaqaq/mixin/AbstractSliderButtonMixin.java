package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Replaces the vanilla slider face (track + handle) with the Skija one on themed menu pages.
@Mixin(AbstractSliderButton.class)
public class AbstractSliderButtonMixin {

	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
	private void zeroday$renderSlider(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		Screen screen = Minecraft.getInstance().gui.screen();
		if (screen == null || !MenuTheme.bakesChrome(screen)) {
			return;
		}
		AbstractSliderButton slider = (AbstractSliderButton) (Object) this;
		MenuTheme.renderSlider(graphics, slider, ((AbstractSliderButtonAccessor) slider).getValue(), mouseX, mouseY);
		ci.cancel();
	}
}
