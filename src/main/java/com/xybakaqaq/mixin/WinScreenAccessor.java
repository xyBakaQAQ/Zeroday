package com.xybakaqaq.mixin;

import net.minecraft.client.gui.screens.WinScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Exposes WinScreen's poem flag: the credits roll takes our menu background, the end poem keeps the
// end-portal sky it was written for.
@Mixin(WinScreen.class)
public interface WinScreenAccessor {

	@Accessor("poem")
	boolean zeroday$poem();
}
