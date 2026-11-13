package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuTheme;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
public class AbstractSelectionListMixin {

	@Inject(method = "extractListBackground", at = @At("HEAD"), cancellable = true)
	private void zeroday$skipListBackground(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (MenuTheme.suppressListBackground()) {
			ci.cancel();
		}
	}

	@Inject(method = "extractListSeparators", at = @At("HEAD"), cancellable = true)
	private void zeroday$skipListSeparators(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (MenuTheme.suppressListBackground()) {
			ci.cancel();
		}
	}
}
