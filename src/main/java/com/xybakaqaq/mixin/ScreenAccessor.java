package com.xybakaqaq.mixin;

import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// Lets the main menu rebuild the title screen's widgets after toggling the custom menu.
@Mixin(Screen.class)
public interface ScreenAccessor {

	@Invoker("rebuildWidgets")
	void zeroday$rebuildWidgets();
}
