package com.xybakaqaq.mixin;

import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.Chat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the vanilla chat message list while the {@link Chat} module is enabled — the
 * Skija-rendered {@code ChatWidget} replaces it (both the unfocused HUD pass and the focused
 * {@code ChatScreen} pass funnel through here).
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {

	@Inject(
			method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private void zeroday$replaceChat(GuiGraphicsExtractor graphics, Font font, int ticks, int mouseX, int mouseY,
			ChatComponent.DisplayMode displayMode, boolean changeCursorOnInsertions, CallbackInfo ci) {
		Chat chat = ModuleManager.get(Chat.class);
		if (chat != null && chat.isEnabled()) {
			ci.cancel();
		}
	}
}
