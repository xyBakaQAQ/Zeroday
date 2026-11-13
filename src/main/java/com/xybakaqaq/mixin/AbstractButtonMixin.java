package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Swaps every plain button face for the Skija one on themed menu pages. Drawn in place, so it also
// reaches the buttons nested inside option lists (cycle buttons) that a screen-level pass would miss.
@Mixin(AbstractButton.class)
public class AbstractButtonMixin {

	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
	private void zeroday$renderButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		AbstractButton button = (AbstractButton) (Object) this;
		Screen screen = Minecraft.getInstance().gui.screen();
		if (screen == null || !button.visible || !MenuTheme.bakesChrome(screen) || !MenuTheme.skinnable(button)) {
			return;
		}
		MenuTheme.renderButton(graphics, button, mouseX, mouseY);
		ci.cancel();
	}
}
