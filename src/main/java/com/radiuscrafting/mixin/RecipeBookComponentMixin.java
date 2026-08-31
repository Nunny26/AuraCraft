package com.radiuscrafting.mixin;

import com.radiuscrafting.client.NearbyStorageWidget;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {

    @Shadow public abstract boolean isVisible();

    @Inject(method = "updateScreenPosition", at = @At("RETURN"), cancellable = true)
    private void radiuscrafting$centerDuoWhenClosed(int width, int imageWidth, CallbackInfoReturnable<Integer> cir) {
        // Only alter the layout if the Recipe Book is CLOSED and the Custom UI is OPEN.
        if (this.isVisible() || !NearbyStorageWidget.isCustomUiOpen) {
            return;
        }

        int vanillaLeftPos = cir.getReturnValue();
        int shiftAmount = 98; // Subtract 98 to perfectly center the Duo!

        cir.setReturnValue(vanillaLeftPos - shiftAmount);
    }
}