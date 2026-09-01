package com.radiuscrafting.client;

import com.radiuscrafting.network.ModMessages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RadiusCraftingClient implements ClientModInitializer {

    // This cache is used by the Mixin to remove the Red Squares in the Recipe Book
    public static List<ItemStack> cachedNearbyItems = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        // Only listen for the nearby items, nothing else!
        ClientPlayNetworking.registerGlobalReceiver(ModMessages.SYNC_NEARBY, (payload, context) -> {
            context.client().execute(() -> {
                cachedNearbyItems = payload.items();
            });
        });
    }
}