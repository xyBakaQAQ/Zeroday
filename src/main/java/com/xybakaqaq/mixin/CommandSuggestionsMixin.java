package com.xybakaqaq.mixin;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import com.xybakaqaq.command.Command;
import com.xybakaqaq.command.CommandManager;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Tab-completion for our chat commands. When the input starts with the command prefix, this
// overrides the suggestions the vanilla updateCommandInfo produced (it already reset the
// visible list) with our command names, or a command's per-argument suggestions.
@Mixin(CommandSuggestions.class)
public class CommandSuggestionsMixin {

	@Shadow
	@Final
	private EditBox input;

	@Shadow
	private CompletableFuture<Suggestions> pendingSuggestions;

	@Shadow
	private boolean keepSuggestions;

	@Inject(method = "updateCommandInfo", at = @At("TAIL"))
	private void zeroday$suggest(CallbackInfo ci) {
		if (keepSuggestions || !CommandManager.enabled()) {
			return;
		}
		String text = input.getValue();
		String upToCursor = text.substring(0, Math.min(input.getCursorPosition(), text.length()));
		String prefix = CommandManager.prefix();
		if (!upToCursor.startsWith(prefix)) {
			return;
		}

		// Offset of the word being completed, measured in the full input.
		String afterPrefix = upToCursor.substring(prefix.length());
		int wordStart = prefix.length() + afterPrefix.lastIndexOf(' ') + 1;

		List<String> candidates;
		if (wordStart == prefix.length()) {
			candidates = CommandManager.labels(); // completing the command name
		} else {
			String[] tokens = afterPrefix.split(" ", -1);
			Command command = CommandManager.get(tokens[0]);
			if (command == null) {
				return;
			}
			candidates = command.complete(Arrays.copyOfRange(tokens, 1, tokens.length));
		}

		if (!candidates.isEmpty()) {
			this.pendingSuggestions = SharedSuggestionProvider.suggest(candidates, new SuggestionsBuilder(upToCursor, wordStart));
		}
	}
}
