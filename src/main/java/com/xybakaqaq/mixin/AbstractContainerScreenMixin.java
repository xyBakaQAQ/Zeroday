package com.xybakaqaq.mixin;

import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.player.InventoryHelper;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Draws the InventoryHelper slot highlight on top of each rendered slot.
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

	@Inject(method = "extractSlot", at = @At("TAIL"))
	private void zeroday$highlightSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		InventoryHelper module = ModuleManager.get(InventoryHelper.class);
		if (module == null) {
			return;
		}
		int color = module.getSlotHighlightColor(slot);
		if (color != 0) {
			graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
		}
	}
}
