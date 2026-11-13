package com.xybakaqaq.mixin;

import com.xybakaqaq.event.EventManager;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Keybind entry point: forwards every raw GLFW key event from KeyboardHandler.keyPress
// to EventManager#onKey.
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

	@Inject(method = "keyPress", at = @At("HEAD"))
	private void zeroday$onKeyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
		EventManager.onKey(event.key(), action);
	}
}
