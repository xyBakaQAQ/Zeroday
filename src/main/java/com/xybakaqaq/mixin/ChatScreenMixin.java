package com.xybakaqaq.mixin;

import com.xybakaqaq.command.CommandManager;
import com.xybakaqaq.gui.arraylist.ArrayListWidget;
import com.xybakaqaq.gui.chat.ChatWidget;
import com.xybakaqaq.gui.inventory.InventoryWidget;
import com.xybakaqaq.gui.keystrokes.KeystrokesWidget;
import com.xybakaqaq.gui.potions.PotionWidget;
import com.xybakaqaq.gui.targethud.TargetHudWidget;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.Chat;
import com.xybakaqaq.modules.render.HUD;
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
 * and — while the chat bar is open and the mouse is free — routes clicks to the draggable HUD
 * widgets (ArrayList, Potions, Inventory, TargetHUD, Keystrokes).
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
		HUD hud = ModuleManager.get(HUD.class);
		if (hud != null && hud.potions()
				&& PotionWidget.mouseClicked(event.x(), event.y(), event.button())) {
			cir.setReturnValue(true);
			return;
		}
		if (hud != null && hud.arrayList().isEnabled()
				&& ArrayListWidget.mouseClicked(event.x(), event.y(), event.button())) {
			cir.setReturnValue(true);
			return;
		}
		if (hud != null && hud.inventory()
				&& InventoryWidget.mouseClicked(event.x(), event.y(), event.button())) {
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
			return;
		}
		Chat chat = ModuleManager.get(Chat.class);
		if (chat != null && chat.isEnabled()
				&& ChatWidget.mouseClicked(event.x(), event.y(), event.button())) {
			cir.setReturnValue(true);
		}
	}
}
