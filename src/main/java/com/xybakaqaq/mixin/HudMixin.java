package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.arraylist.ArrayListWidget;
import com.xybakaqaq.gui.chat.ChatWidget;
import com.xybakaqaq.gui.hotbar.HotbarManager;
import com.xybakaqaq.gui.inventory.InventoryWidget;
import com.xybakaqaq.gui.keystrokes.KeystrokesWidget;
import com.xybakaqaq.gui.potions.PotionWidget;
import com.xybakaqaq.gui.targethud.TargetHudWidget;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.Chat;
import com.xybakaqaq.modules.render.Crosshair;
import com.xybakaqaq.modules.render.HUD;
import com.xybakaqaq.modules.render.Keystrokes;
import com.xybakaqaq.modules.render.TargetHUD;
import com.xybakaqaq.utils.HudLayout;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the vanilla hotbar render so the mod's {@link HotbarManager} can optionally
 * replace it. {@code Hud.extractItemHotbar} is the method that lays out the hotbar
 * background, selection, slots and offhand each frame (MC 26.2 render-state model).
 *
 * <p>Also nudges the bottom status bars (health, armor, food, air, vehicle health) down by
 * {@link HudLayout#STATUS_OFFSET} so they fill the strip vacated by the floating hotbar.
 * Each of these methods anchors to the bottom via a single {@code guiHeight()} call.
 */
@Mixin(Hud.class)
public class HudMixin {

	@Shadow
	private int toolHighlightTimer;

	@Shadow
	private ItemStack lastToolHighlight;

	@Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
	private void zeroday$onExtractHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (HotbarManager.onExtractHotbar(graphics, deltaTracker)) {
			ci.cancel();
		}
	}

	@Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
	private void zeroday$onExtractCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		Crosshair crosshair = ModuleManager.get(Crosshair.class);
		if (crosshair != null && crosshair.isEnabled()
				&& Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
			Crosshair.render(graphics);
			ci.cancel();
		}
	}

	@Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
	private void zeroday$onExtractSelectedItemName(GuiGraphicsExtractor graphics, CallbackInfo ci) {
		if (HotbarManager.onExtractSelectedItemName(graphics, this.lastToolHighlight, this.toolHighlightTimer)) {
			ci.cancel();
		}
	}

	@Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
	private void zeroday$onExtractHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		HUD hud = ModuleManager.get(HUD.class);
		Minecraft mc = Minecraft.getInstance();
		boolean chatOpen = mc.gui.screen() instanceof ChatScreen;
		if (hud != null && hud.arrayList().isEnabled()) {
			ArrayListWidget.render(graphics, chatOpen);
		}
		if (hud != null && hud.potions()) {
			PotionWidget.render(graphics, chatOpen);
		}
		if (hud != null && hud.inventory()) {
			InventoryWidget.render(graphics, chatOpen);
		}
		TargetHUD targetHud = ModuleManager.get(TargetHUD.class);
		if (targetHud != null && targetHud.isEnabled()) {
			TargetHudWidget.render(graphics, chatOpen);
		}
		Keystrokes keystrokes = ModuleManager.get(Keystrokes.class);
		if (keystrokes != null && keystrokes.isEnabled()) {
			KeystrokesWidget.render(graphics, chatOpen);
		}
		Chat chat = ModuleManager.get(Chat.class);
		if (chat != null && chat.isEnabled()) {
			ChatWidget.render(graphics, chatOpen);
		}
	}

	// Suppress the vanilla potion display when our custom one is on; otherwise push it below the ArrayList.
	@Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
	private void zeroday$pushEffectsDown(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		HUD hud = ModuleManager.get(HUD.class);
		if (hud != null && hud.potions()) {
			ci.cancel();
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(0, hud == null ? 0 : hud.arrayListHeight());
	}

	@Inject(method = "extractEffects", at = @At("RETURN"))
	private void zeroday$popEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		graphics.pose().popMatrix();
	}

	@Redirect(
			method = "extractPlayerHealth",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;guiHeight()I")
	)
	private int zeroday$shiftHealthDown(GuiGraphicsExtractor graphics) {
		return HudLayout.shiftDown(graphics.guiHeight());
	}

	@Redirect(
			method = "extractVehicleHealth",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;guiHeight()I")
	)
	private int zeroday$shiftVehicleHealthDown(GuiGraphicsExtractor graphics) {
		return HudLayout.shiftDown(graphics.guiHeight());
	}
}
