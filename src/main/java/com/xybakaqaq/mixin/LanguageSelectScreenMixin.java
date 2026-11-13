package com.xybakaqaq.mixin;

import java.util.concurrent.CompletableFuture;

import com.xybakaqaq.utils.LanguageReloader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Switching the language used to go through Minecraft.reloadResourcePacks(): the pack repository is
// rebuilt from disk and every texture, model, sound and font is reloaded, so a language change sat
// behind the loading overlay for seconds. Only the language files are re-read instead.
@Mixin(LanguageSelectScreen.class)
public class LanguageSelectScreenMixin {

	@Redirect(method = "onDone", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;"))
	private CompletableFuture<Void> zeroday$reloadLanguageOnly(Minecraft minecraft) {
		return LanguageReloader.reloadLanguageOnly(minecraft);
	}
}
