package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Option list headers ("DISPLAY", "QUALITY", ...) are FocusableTextWidgets drawn on their own; give
// them the menu font too. A focused header keeps its vanilla text so the focus ring stays readable.
@Mixin(FocusableTextWidget.class)
public class FocusableTextWidgetMixin {

	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
	private void zeroday$renderText(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		FocusableTextWidget widget = (FocusableTextWidget) (Object) this;
		Screen screen = Minecraft.getInstance().gui.screen();
		if (screen == null || !MenuTheme.bakesChrome(screen) || widget.isFocused()) {
			return;
		}
		String text = widget.getMessage().getString();
		if (text.isBlank() || widget.getWidth() <= 8 || widget.getHeight() < 8) {
			return;
		}
		MenuTheme.renderText(graphics, widget, text, Math.max(0, widget.getPadding()));
		ci.cancel();
	}
}
