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
                // If they have a container open (like Crafting Screen or standard Inventory Screen with Recipe Book)
                // Actually we just sync unconditionally or when they are alive?
                // The prompt says: "Every 20 ticks (1 second), generate the list of items using RadiusCraftingLogic.getSources(...), extract all the item stacks, and call ModMessages.sendSyncNearbyItems(player, allItems);"
                List<ItemStack> allItems = RadiusCraftingLogic.getAllAvailableItems(
                    player,
                    com.radiuscrafting.RadiusCrafting.CONFIG.searchRadius,
                    com.radiuscrafting.RadiusCrafting.CONFIG.useEnderChest,
                    com.radiuscrafting.RadiusCrafting.CONFIG.useShulkerBoxes
                );
                ModMessages.sendSyncNearbyItems(player, allItems);
            } else if (player.containerMenu == player.inventoryMenu) {
                // Still sync for normal inventory because nearby storage widget appears there too!
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
