package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuAnimation;
import com.xybakaqaq.gui.mainmenu.MenuListPage;
import com.xybakaqaq.gui.mainmenu.MenuTheme;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Custom menu hooks for every Screen.
@Mixin(Screen.class)
public class ScreenMixin {

	// Swap the vanilla background for the Skija one (opaque, so the dirt/blur would be wasted).
	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void zeroday$renderMenuBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		Screen screen = (Screen) (Object) this;
		if (MenuTheme.bakesChrome(screen)) {
			MenuTheme.renderBackground(screen, graphics, mouseX, mouseY);
			ci.cancel();
		} else if (MenuListPage.handles(screen)) {
			ci.cancel();
		}
	}

	// World select / multiplayer pages are drawn entirely by MenuListPage.
	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void zeroday$renderMenuListPage(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		Screen screen = (Screen) (Object) this;
		if (MenuListPage.handles(screen)) {
			MenuListPage.render(screen, graphics, mouseX, mouseY, partialTick);
			ci.cancel();
		}
	}

	// Starts the frame clock for the menu animations.
	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
	private void zeroday$beginMenuFrame(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		MenuAnimation.begin((Screen) (Object) this);
	}
}
