package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuTheme;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// PauseScreen draws its own background (blur + the in-world menu texture) instead of going through
// Screen#extractBackground, so the generic hook never sees the ESC menu. Replace it with the themed
// wash while the Theme module is on.
@Mixin(PauseScreen.class)
public class PauseScreenMixin {

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void zeroday$renderThemedBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		PauseScreen screen = (PauseScreen) (Object) this;
		if (!screen.showsPauseMenu() || !MenuTheme.bakesChrome(screen)) {
			return;
		}
		MenuTheme.renderBackground(screen, graphics, mouseX, mouseY);
		ci.cancel();
	}
}
