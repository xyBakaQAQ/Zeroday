package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuListPage;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Applies our own geometry to the world select page after the vanilla layout has run.
@Mixin(SelectWorldScreen.class)
public class SelectWorldScreenMixin {

	@Inject(method = "init", at = @At("TAIL"))
	private void zeroday$layout(CallbackInfo ci) {
		MenuListPage.layout((Screen) (Object) this);
	}

	@Inject(method = "repositionElements", at = @At("TAIL"))
	private void zeroday$relayout(CallbackInfo ci) {
		MenuListPage.layout((Screen) (Object) this);
	}
}
