package com.radiuscrafting.mixin;

import com.radiuscrafting.logic.RadiusCraftingLogic;
import com.radiuscrafting.network.ModMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Unique
    private int radiusCrafting$tickCounter = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void radiusCrafting$onTickTail(CallbackInfo ci) {
        radiusCrafting$tickCounter++;
        if (radiusCrafting$tickCounter >= 20) {
            radiusCrafting$tickCounter = 0;
            ServerPlayer player = (ServerPlayer) (Object) this;
            if (player.containerMenu != null && player.containerMenu != player.inventoryMenu) {
                List<ItemStack> allItems = RadiusCraftingLogic.getAllAvailableItems(
                    player,
                    com.radiuscrafting.RadiusCrafting.CONFIG.searchRadius,
                    com.radiuscrafting.RadiusCrafting.CONFIG.useEnderChest,
                    com.radiuscrafting.RadiusCrafting.CONFIG.useShulkerBoxes
                );
                ModMessages.sendSyncNearbyItems(player, allItems);
            } else if (player.containerMenu == player.inventoryMenu) {
                List<ItemStack> allItems = RadiusCraftingLogic.getAllAvailableItems(
                    player,
                    com.radiuscrafting.RadiusCrafting.CONFIG.searchRadius,
                    com.radiuscrafting.RadiusCrafting.CONFIG.useEnderChest,
                    com.radiuscrafting.RadiusCrafting.CONFIG.useShulkerBoxes
                );
                ModMessages.sendSyncNearbyItems(player, allItems);
            }
        }
    }
}
