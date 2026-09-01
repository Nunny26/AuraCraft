package com.radiuscrafting.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ModMessages {

    public static final CustomPacketPayload.Type<SyncNearbyItemsPayload> SYNC_NEARBY = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("radiuscrafting", "sync_nearby"));

    public record SyncNearbyItemsPayload(List<ItemStack> items) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncNearbyItemsPayload> CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
                SyncNearbyItemsPayload::items,
                SyncNearbyItemsPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return SYNC_NEARBY;
        }
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(SYNC_NEARBY, SyncNearbyItemsPayload.CODEC);
    }

    public static void sendSyncNearbyItems(ServerPlayer player, List<ItemStack> items) {
        ServerPlayNetworking.send(player, new SyncNearbyItemsPayload(items));
    }
}