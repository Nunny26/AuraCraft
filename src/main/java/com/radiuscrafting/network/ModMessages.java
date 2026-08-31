package com.radiuscrafting.network;

import com.radiuscrafting.logic.RadiusCraftingLogic;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.inventory.ContainerInput;

import java.util.ArrayList;
import java.util.List;

public class ModMessages {

    public static final Identifier SYNC_ID = Identifier.fromNamespaceAndPath("radiuscrafting", "sync_items");
    public static final Identifier SYNC_STORAGE_ID = Identifier.fromNamespaceAndPath("radiuscrafting", "sync_storage");
    public static final Identifier REQUEST_STORAGE_ID = Identifier.fromNamespaceAndPath("radiuscrafting", "request_storage");

    public record SyncNearbyItemsPayload(List<ItemStack> items) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncNearbyItemsPayload> ID = new CustomPacketPayload.Type<>(SYNC_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncNearbyItemsPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.items().size());
                for (ItemStack stack : payload.items()) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<ItemStack> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                }
                return new SyncNearbyItemsPayload(list);
            }
        );

        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }
    
    public record StorageUIRecord(String id, net.minecraft.network.chat.Component displayName, List<ItemStack> items) {}

    public record SyncStorageUIPayload(List<StorageUIRecord> records) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncStorageUIPayload> ID = new CustomPacketPayload.Type<>(SYNC_STORAGE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncStorageUIPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.records().size());
                for (StorageUIRecord r : payload.records()) {
                    buf.writeUtf(r.id());
                    ComponentSerialization.STREAM_CODEC.encode(buf, r.displayName());
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf, r.items());
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<StorageUIRecord> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(new StorageUIRecord(buf.readUtf(), ComponentSerialization.STREAM_CODEC.decode(buf), ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buf)));
                }
                return new SyncStorageUIPayload(list);
            }
        );
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record RequestStorageUIC2SPacket() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestStorageUIC2SPacket> ID = new CustomPacketPayload.Type<>(REQUEST_STORAGE_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestStorageUIC2SPacket> CODEC = StreamCodec.unit(new RequestStorageUIC2SPacket());
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public static final Identifier STORAGE_CLICK_ID = Identifier.fromNamespaceAndPath("radiuscrafting", "storage_click");
    public record StorageClickC2SPacket(String storageId, int slotIndex, int button, ContainerInput actionType) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StorageClickC2SPacket> ID = new CustomPacketPayload.Type<>(STORAGE_CLICK_ID);
        public static final StreamCodec<RegistryFriendlyByteBuf, StorageClickC2SPacket> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.storageId());
                buf.writeInt(payload.slotIndex());
                buf.writeInt(payload.button());
                buf.writeEnum(payload.actionType());
            },
            buf -> new StorageClickC2SPacket(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readEnum(ContainerInput.class))
        );
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public static void registerC2SPackets() {
        PayloadTypeRegistry.serverboundPlay().register(RequestStorageUIC2SPacket.ID, RequestStorageUIC2SPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StorageClickC2SPacket.ID, StorageClickC2SPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncNearbyItemsPayload.ID, SyncNearbyItemsPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncStorageUIPayload.ID, SyncStorageUIPayload.CODEC);
        
        ServerPlayNetworking.registerGlobalReceiver(RequestStorageUIC2SPacket.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                sendSyncPackets(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(StorageClickC2SPacket.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                RadiusCraftingLogic.handleVirtualClick(player, payload.storageId(), payload.slotIndex());
                sendSyncPackets(player);
            });
        });
    }

    public static void registerS2CPackets() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(SyncNearbyItemsPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                com.radiuscrafting.client.RadiusCraftingClient.cachedNearbyItems = payload.items();
            });
        });

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(SyncStorageUIPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                com.radiuscrafting.client.RadiusCraftingClient.cachedStorageRecords = payload.records();
            });
        });
    }

    public static void sendSyncPackets(ServerPlayer player) {
        RadiusCraftingLogic.LOGGER.info("Syncing UI for player {}", player.getName().getString());
        List<RadiusCraftingLogic.ItemSource> sources = RadiusCraftingLogic.getSources(player, com.radiuscrafting.RadiusCrafting.CONFIG.searchRadius, com.radiuscrafting.RadiusCrafting.CONFIG.useEnderChest, com.radiuscrafting.RadiusCrafting.CONFIG.useShulkerBoxes);
        
        List<StorageUIRecord> records = new java.util.ArrayList<>();
        for (RadiusCraftingLogic.ItemSource source : sources) {
            if ("Player Inventory".equals(source.getStorageId())) continue;
            records.add(new StorageUIRecord(source.getStorageId(), source.getDisplayName(), source.getAllItems()));
        }
        
        ServerPlayNetworking.send(player, new SyncStorageUIPayload(records));
        
        List<ItemStack> allItems = new java.util.ArrayList<>();
        for (RadiusCraftingLogic.ItemSource source : sources) {
            allItems.addAll(source.getAllItems());
        }
        ServerPlayNetworking.send(player, new SyncNearbyItemsPayload(allItems));
    }
}
