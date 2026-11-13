package com.xybakaqaq.mixin;

import com.xybakaqaq.gui.mainmenu.MenuTextField;
import com.xybakaqaq.gui.mainmenu.MenuTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Text fields on the themed pages (the language and resource-pack searches) are drawn the same way as
// the ones on our own pages: Skija text with a hairline underline instead of the vanilla frame. Input
// handling stays with the vanilla EditBox. The list pages draw their own field (MenuListPage), so
// their boxes are left alone here.
@Mixin(EditBox.class)
public class EditBoxMixin {

	@Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
	private void zeroday$renderSkijaField(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		EditBox box = (EditBox) (Object) this;
		if (!box.visible || !MenuTheme.bakesChrome(Minecraft.getInstance().gui.screen())) {
			return;
		}
		// Our field has no frame, and an unframed EditBox measures its text from the left edge, which
		// also keeps the click-to-caret mapping in line with what is drawn.
		if (box.isBordered()) {
			box.setBordered(false);
		}
		MenuTextField.drawField(graphics, box);
		ci.cancel();
	}
}
