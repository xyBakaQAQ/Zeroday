package com.xybakaqaq.mixin;

import com.xybakaqaq.command.CommandManager;
import com.xybakaqaq.gui.cloudmusic.CloudMusicWidget;
import com.xybakaqaq.gui.keystrokes.KeystrokesWidget;
import com.xybakaqaq.gui.targethud.TargetHudWidget;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.CloudMusic;
import com.xybakaqaq.modules.render.Keystrokes;
import com.xybakaqaq.modules.render.TargetHUD;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes sent chat input through {@link CommandManager} (commands are handled locally, not sent),
 * and — while the chat bar is open and the mouse is free — routes clicks to the CloudMusic
 * now-playing widget (controls + right-click to open the full UI).
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {

	@Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
	private void zeroday$onChatInput(String msg, boolean addToRecent, CallbackInfo ci) {
		if (CommandManager.dispatch(msg)) {
			ci.cancel();
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void zeroday$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		CloudMusic cloudMusic = ModuleManager.get(CloudMusic.class);
		if (cloudMusic != null && cloudMusic.isEnabled()
				&& CloudMusicWidget.mouseClicked(event.x(), event.y(), event.button())) {
			cir.setReturnValue(true);
			return;
		}
		TargetHUD targetHud = ModuleManager.get(TargetHUD.class);
		if (targetHud != null && targetHud.isEnabled()
				&& TargetHudWidget.mouseClicked(event.x(), event.y(), event.button())) {
			cir.setReturnValue(true);
			return;
		}
		Keystrokes keystrokes = ModuleManager.get(Keystrokes.class);
		if (keystrokes != null && keystrokes.isEnabled()
				&& KeystrokesWidget.mouseClicked(event.x(), event.y(), event.button())) {
			cir.setReturnValue(true);
		}
	}
}
