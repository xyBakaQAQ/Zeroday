package com.xybakaqaq.mixin;

import java.util.List;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Read access to the chat log's wrapped lines for the Skija-rendered ChatWidget.
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {

	@Accessor("trimmedMessages")
	List<GuiMessage.Line> getTrimmedMessages();

	@Accessor("chatScrollbarPos")
	int getChatScrollbarPos();
}
