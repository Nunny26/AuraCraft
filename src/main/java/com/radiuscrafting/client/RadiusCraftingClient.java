package com.radiuscrafting.client;

import com.radiuscrafting.network.ModMessages;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RadiusCraftingClient implements ClientModInitializer {
    public static List<ItemStack> cachedNearbyItems = new ArrayList<>();
    public static List<ModMessages.StorageUIRecord> cachedStorageRecords = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        ModMessages.registerS2CPackets();
        RadiusCraftingNames.load();
    }
}
