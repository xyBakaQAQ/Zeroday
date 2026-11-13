package com.xybakaqaq.mixin;

import java.util.List;

import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Closing the resource-pack screen commits the selection unconditionally: the repository's selected
// list is rebuilt and options.txt is written again even when nothing was moved. Vanilla already only
// reloads the resources when the resulting list differs, so when it cannot differ we skip the commit
// (and the work before it) entirely.
@Mixin(PackSelectionModel.class)
public class PackSelectionModelMixin {

	@Shadow
	private List<Pack> selected;

	@Shadow
	private PackRepository repository;

	@Inject(method = "commit", at = @At("HEAD"), cancellable = true)
	private void zeroday$skipUnchangedCommit(CallbackInfo ci) {
		List<Pack> applied = List.copyOf(repository.getSelectedPacks());
		if (applied.size() != selected.size()) {
			return;
		}
		// The display list runs highest priority first; the repository keeps the opposite order.
		for (int i = 0; i < selected.size(); i++) {
			if (!selected.get(i).getId().equals(applied.get(applied.size() - 1 - i).getId())) {
				return;
			}
		}
		ci.cancel();
	}
}
