package com.xybakaqaq.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Text field state the Skija renderer needs but EditBox keeps private.
@Mixin(EditBox.class)
public interface EditBoxAccessor {

	@Accessor("highlightPos")
	int zeroday$highlightPos();

	@Accessor("hint")
	Component zeroday$hint();
}
