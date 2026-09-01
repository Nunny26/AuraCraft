package com.radiuscrafting.mixin;

import com.radiuscrafting.client.RadiusCraftingClient;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class PlayerInventoryMixin {

    @Shadow public Player player;

    @Inject(method = "fillStackedContents", at = @At("TAIL"))
    private void radiusCrafting$populateRecipeFinder(net.minecraft.world.entity.player.StackedItemContents finder, CallbackInfo ci) {
        if (this.player.level().isClientSide()) {
            for (ItemStack stack : RadiusCraftingClient.cachedNearbyItems) {
                finder.accountStack(stack);
            }
        }
    }
}
