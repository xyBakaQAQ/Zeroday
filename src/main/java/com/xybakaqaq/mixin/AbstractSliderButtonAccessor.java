package com.xybakaqaq.mixin;

import net.minecraft.client.gui.components.AbstractSliderButton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Read access to the slider's normalized value for the Skija slider skin.
@Mixin(AbstractSliderButton.class)
public interface AbstractSliderButtonAccessor {

	@Accessor("value")
	double getValue();
}
