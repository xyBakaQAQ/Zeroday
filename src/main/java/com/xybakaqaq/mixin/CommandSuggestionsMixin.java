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

/**
 * Tab-completion for our chat commands. When the input starts with the command prefix, this
 * overrides the suggestions the vanilla {@code updateCommandInfo} produced (it already reset the
 * visible list) with our command names, or a command's per-argument suggestions.
 */
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
		if (keepSuggestions) {
			return;
		}
		String text = input.getValue();
		String prefix = CommandManager.PREFIX;
		if (!text.startsWith(prefix)) {
			return;
		}

		int cursor = Math.min(input.getCursorPosition(), text.length());
		String upToCursor = text.substring(0, cursor);
		if (upToCursor.length() < prefix.length()) {
			return;
		}

		String afterPrefix = upToCursor.substring(prefix.length());
		int wordStartInAfter = afterPrefix.lastIndexOf(' ') + 1;
		int wordStart = prefix.length() + wordStartInAfter;

		List<String> candidates;
		if (wordStartInAfter == 0) {
			// completing the command name
			candidates = CommandManager.getNames();
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
