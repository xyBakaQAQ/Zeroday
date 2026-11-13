package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuTheme;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Replaces the nether-portal loading background with the menu gradient (the progress bar and text
// above it stay vanilla).
@Mixin(LevelLoadingScreen.class)
public class LevelLoadingScreenMixin {

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void zeroday$renderLoadingBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		LevelLoadingScreen screen = (LevelLoadingScreen) (Object) this;
		if (MenuTheme.bakesChrome(screen)) {
			MenuTheme.renderBackground(screen, graphics, mouseX, mouseY);
			ci.cancel();
		}
	}
}
