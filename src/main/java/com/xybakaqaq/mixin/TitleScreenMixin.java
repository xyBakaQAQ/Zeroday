package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MainMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the title screen with the fully custom Skija {@link MainMenu}: the vanilla buttons are
 * re-laid-out into the menu's stack on init (so they still handle clicks), and the vanilla
 * render-state extraction is cancelled — {@link MainMenu#render} draws everything instead.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin {

	@Inject(method = "init", at = @At("TAIL"))
	private void zeroday$layoutMainMenu(CallbackInfo ci) {
		MainMenu.layout((TitleScreen) (Object) this);
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void zeroday$renderMainMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		MainMenu.render((TitleScreen) (Object) this, graphics, mouseX, mouseY);
		ci.cancel();
	}
}
