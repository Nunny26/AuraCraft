package com.radiuscrafting.mixin;

import com.radiuscrafting.RadiusCrafting;
import com.radiuscrafting.logic.RadiusCraftingLogic;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(net.minecraft.server.network.ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin {

    @Shadow public ServerPlayer player;
    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"))
    private void radiusCrafting$onCraftRequest(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        if (!((net.minecraft.server.level.ServerLevel)this.player.level()).getServer().isSameThread()) {
            return;
        }
        
        if (packet.containerId() == this.player.containerMenu.containerId) {
            net.minecraft.world.item.crafting.display.RecipeDisplayId recipeId = packet.recipe();
            boolean craftAll = packet.useMaxItems();

            net.minecraft.world.item.crafting.RecipeManager.ServerDisplayInfo displayInfo = ((net.minecraft.server.level.ServerLevel)this.player.level()).getServer().getRecipeManager().getRecipeFromDisplay(recipeId);

            if (displayInfo != null) {
                RecipeHolder<?> recipeEntry = displayInfo.parent();
                if (recipeEntry != null) {
                    RadiusCraftingLogic.LOGGER.info("Intercepted Recipe Book craft request");
                    
                    RadiusCraftingLogic.pullItemsToInventory(
                        this.player, 
                        recipeEntry.value().placementInfo(), 
                        craftAll, 
                        RadiusCrafting.CONFIG.searchRadius,
                        RadiusCrafting.CONFIG.useEnderChest,
                        RadiusCrafting.CONFIG.useShulkerBoxes
                    );
                }
            } else {
                RadiusCraftingLogic.LOGGER.warn("Could not find recipe with display ID");
            }
        }
    }
}
