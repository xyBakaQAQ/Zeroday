package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuListPage;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Applies our own geometry to the multiplayer page after the vanilla layout has run.
@Mixin(JoinMultiplayerScreen.class)
public class JoinMultiplayerScreenMixin {

	@Inject(method = "init", at = @At("TAIL"))
	private void zeroday$layout(CallbackInfo ci) {
		MenuListPage.layout((Screen) (Object) this);
	}

	@Inject(method = "repositionElements", at = @At("TAIL"))
	private void zeroday$relayout(CallbackInfo ci) {
		MenuListPage.layout((Screen) (Object) this);
	}
}
