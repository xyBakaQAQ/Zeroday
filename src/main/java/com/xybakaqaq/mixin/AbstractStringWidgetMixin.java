package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Page titles are plain StringWidgets; re-draw them in the menu font so they match the skinned
// buttons. Other string widgets (multi-line labels, checkboxes) keep their vanilla text.
@Mixin(AbstractStringWidget.class)
public class AbstractStringWidgetMixin {

	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
	private void zeroday$renderText(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		AbstractStringWidget widget = (AbstractStringWidget) (Object) this;
		if (!(widget instanceof StringWidget) || !widget.visible) {
			return;
		}
		Screen screen = Minecraft.getInstance().gui.screen();
		if (screen == null || !MenuTheme.bakesChrome(screen)) {
			return;
		}
		String text = widget.getMessage().getString();
		if (text.isBlank() || widget.getWidth() <= 8 || widget.getHeight() < 8) {
			return;
		}
		MenuTheme.renderText(graphics, widget, text, 0f);
		ci.cancel();
	}
}
