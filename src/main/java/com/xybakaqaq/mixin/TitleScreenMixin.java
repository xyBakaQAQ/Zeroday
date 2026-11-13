package com.xybakaqaq.mixin;

import com.xybakaqaq.config.UiConfig;
import com.xybakaqaq.gui.mainmenu.MainMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

	@Inject(method = "init", at = @At("TAIL"))
	private void zeroday$layoutMainMenu(CallbackInfo ci) {
		if (UiConfig.customMenu()) {
			MainMenu.layout((TitleScreen) (Object) this);
		}
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void zeroday$renderMainMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (UiConfig.customMenu()) {
			MainMenu.render((TitleScreen) (Object) this, graphics, mouseX, mouseY);
			ci.cancel();
		}
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void zeroday$renderMenuToggle(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		// Only reached when the vanilla menu rendered (the HEAD injection cancels otherwise).
		if (!UiConfig.customMenu()) {
			MainMenu.drawToggle(graphics, mouseX, mouseY);
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void zeroday$toggleMenu(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		if (event.button() == 1 && MainMenu.toggleClicked((TitleScreen) (Object) this, event.x(), event.y())) {
			cir.setReturnValue(true);
		}
	}
}
