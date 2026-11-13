package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuListPage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The vanilla row width is a fixed 270, which leaves the entries floating in the middle of a widened
// list. On our page the rows fill the list instead, so the page can use the whole window.
@Mixin(WorldSelectionList.class)
public class WorldSelectionListMixin {

	@Inject(method = "getRowWidth", at = @At("RETURN"), cancellable = true)
	private void zeroday$fillRowWidth(CallbackInfoReturnable<Integer> cir) {
		if (!MenuListPage.handles(Minecraft.getInstance().gui.screen())) {
			return;
		}
		int width = ((AbstractSelectionList<?>) (Object) this).getWidth();
		cir.setReturnValue(Math.max(200, width - 24));
	}
}
