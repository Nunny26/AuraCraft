package com.radiuscrafting.mixin;

import com.radiuscrafting.client.RadiusCraftingClient;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.StackedContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.player.Inventory.class)
public class PlayerInventoryMixin {
    @Inject(method = "fillStackedContents", at = @At("TAIL"))
    private void radiusCrafting$populateRecipeFinder(net.minecraft.world.entity.player.StackedItemContents finder, CallbackInfo ci) {
        // Inject all the cached nearby items into the client's RecipeFinder.
        // This removes the "Red Square" because the client thinks it has these items.
        for (ItemStack stack : RadiusCraftingClient.cachedNearbyItems) {
            finder.accountStack(stack);
        }
    }
}
