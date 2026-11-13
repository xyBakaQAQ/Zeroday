package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuListPage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Same as WorldSelectionListMixin: the fixed 305px row becomes the full list width on our page.
@Mixin(ServerSelectionList.class)
public class ServerSelectionListMixin {

	@Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
	private void zeroday$fillRowWidth(CallbackInfoReturnable<Integer> cir) {
		if (!MenuListPage.handles(Minecraft.getInstance().gui.screen())) {
			return;
		}
		int width = ((AbstractSelectionList<?>) (Object) this).getWidth();
		cir.setReturnValue(Math.max(200, width - 24));
	}
}
