package com.xybakaqaq.mixin;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.Chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Suppresses the vanilla chat message list while the Chat module is enabled — the
// Skija-rendered ChatWidget replaces it (both the unfocused HUD pass and the focused
// ChatScreen pass funnel through here). Also applies the module's history size and the
// "keep history" toggle to the vanilla message store.
@Mixin(ChatComponent.class)
public class ChatComponentMixin {

	// Vanilla's hard-coded history cap (both the message count and the wrapped-line count).
	@Unique
	private static final int VANILLA_HISTORY = 100;

	@Shadow
	private List<GuiMessage> allMessages;

	@Shadow
	private List<GuiMessage.Line> trimmedMessages;

	@Shadow
	private int chatScrollbarPos;

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

	// while (allMessages.size() > 100) removeLast() — honour the configured cap instead.
	@ModifyConstant(method = "addMessageToQueue", constant = @Constant(intValue = VANILLA_HISTORY))
	private int zeroday$messageCap(int vanilla) {
		return cap();
	}

	// while (trimmedMessages.size() > 100) removeLast() — same, for wrapped lines.
	@ModifyConstant(method = "addMessageToDisplayQueue", constant = @Constant(intValue = VANILLA_HISTORY))
	private int zeroday$lineCap(int vanilla) {
		return cap();
	}

	// Vanilla only trims when a message arrives, so lowering the setting would not take effect until
	// the next one. Trim every tick instead (the lists are ordered, so this is a no-op when in range).
	@Inject(method = "tick", at = @At("TAIL"))
	private void zeroday$trimToCap(CallbackInfo ci) {
		int cap = cap();
		trim(allMessages, cap);
		trim(trimmedMessages, cap);
		chatScrollbarPos = Math.max(0, Math.min(chatScrollbarPos, trimmedMessages.size()));
	}

	// Chat log snapshot taken around clearMessages when history is being kept.
	@Unique
	private List<GuiMessage> keptMessages;
	@Unique
	private List<GuiMessage.Line> keptLines;

	// clearMessages(true) is the disconnect/quit path (Hud.onDisconnected); leaving a
	// world wipes the log. Snapshot the log first so it can be put back afterwards — the rest of the
	// method (flushing delayed messages, clearing the deletion queue, resetting input history) still
	// runs, which a plain cancel would skip. false is the debug clear-chat keybind, untouched.
	@Inject(method = "clearMessages(Z)V", at = @At("HEAD"))
	private void zeroday$saveHistory(boolean clearHistory, CallbackInfo ci) {
		keptMessages = null;
		keptLines = null;
		Chat chat = ModuleManager.get(Chat.class);
		if (clearHistory && chat != null && chat.isEnabled() && chat.keepHistory()) {
			keptMessages = new ArrayList<>(allMessages);
			keptLines = new ArrayList<>(trimmedMessages);
		}
	}

	@Inject(method = "clearMessages(Z)V", at = @At("TAIL"))
	private void zeroday$restoreHistory(boolean clearHistory, CallbackInfo ci) {
		if (keptMessages == null) {
			return;
		}
		// Re-stamp the kept messages: their old timestamps are far in the past, so they would come
		// back already faded out. Stamping them now shows them for the usual ten seconds.
		Gui gui = Minecraft.getInstance().gui;
		int now = gui == null ? 0 : gui.hud.getGuiTicks();
		Map<GuiMessage, GuiMessage> fresh = new IdentityHashMap<>();
		for (GuiMessage message : keptMessages) {
			fresh.put(message, new GuiMessage(now, message.content(), message.signature(),
					message.source(), message.tag()));
		}
		for (GuiMessage message : keptMessages) {
			allMessages.add(fresh.get(message));
		}
		for (GuiMessage.Line line : keptLines) {
			GuiMessage parent = fresh.get(line.parent());
			trimmedMessages.add(parent == null ? line
					: new GuiMessage.Line(parent, line.content(), line.endOfEntry()));
		}
		chatScrollbarPos = 0;
		keptMessages = null;
		keptLines = null;
	}

	private static void trim(List<?> list, int cap) {
		while (list.size() > cap) {
			list.remove(list.size() - 1);
		}
	}

	private static int cap() {
		Chat chat = ModuleManager.get(Chat.class);
		return chat == null || !chat.isEnabled() ? VANILLA_HISTORY : Math.max(1, chat.maxLines());
	}
}
