package com.radiuscrafting.logic;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;

public class RadiusCraftingLogic {
    public static final Logger LOGGER = LogUtils.getLogger();

    public interface IItemSource {
        ItemStack extractItem(Ingredient ingredient, int amount, boolean simulate, Item lockedItem);
        ItemStack extractOne(Ingredient ingredient, Item lockedItem);
        int countItem(Ingredient ingredient);
        net.minecraft.network.chat.Component getDisplayName();
        String getStorageId();
        List<ItemStack> getAllItems();
        ItemStack getStackInSlot(int slot);
        void setStackInSlot(int slot, ItemStack stack);
        boolean isShulkerSource();
        void insertItem(ItemStack stack);
    }

    public static class InventorySource implements IItemSource {
        private final Container inventory;
        private final net.minecraft.network.chat.Component displayName;
        private final String storageId;

        public InventorySource(Container inventory, net.minecraft.network.chat.Component displayName, String storageId) {
            this.inventory = inventory;
            this.displayName = displayName;
            this.storageId = storageId;
        }

        @Override
        public ItemStack extractItem(Ingredient ingredient, int amount, boolean simulate, Item lockedItem) {
            ItemStack extracted = ItemStack.EMPTY;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                boolean isValid = lockedItem != null ? stack.is(lockedItem) : ingredient.test(stack);
                if (!stack.isEmpty() && isValid) {
                    int toTake = Math.min(stack.getCount(), amount - extracted.getCount());
                    if (toTake > 0) {
                        if (extracted.isEmpty()) {
                            extracted = stack.copyWithCount(toTake);
                        } else if (ItemStack.isSameItemSameComponents(extracted, stack)) {
                            extracted.grow(toTake);
                        } else {
                            continue;
                        }
                        
                        if (!simulate) {
                            inventory.removeItem(i, toTake);
                        }
                        
                        if (extracted.getCount() >= amount) break;
                    }
                }
            }
            return extracted;
        }

        @Override
        public ItemStack extractOne(Ingredient ingredient, Item lockedItem) {
            return extractItem(ingredient, 1, false, lockedItem);
        }

        @Override
        public int countItem(Ingredient ingredient) {
            int count = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    count += stack.getCount();
                }
            }
            return count;
        }

        @Override
        public List<ItemStack> getAllItems() {
            List<ItemStack> items = new ArrayList<>(inventory.getContainerSize());
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                items.add(inventory.getItem(i).copy());
            }
            return items;
        }

        @Override
        public net.minecraft.network.chat.Component getDisplayName() {
            return displayName;
        }

        @Override
        public String getStorageId() {
            return storageId;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot >= 0 && slot < inventory.getContainerSize()) return inventory.getItem(slot);
            return ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (slot >= 0 && slot < inventory.getContainerSize()) {
                inventory.setItem(slot, stack);
            }
        }

        @Override
        public boolean isShulkerSource() {
            return false;
        }

        @Override
        public void insertItem(ItemStack stack) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack slot = inventory.getItem(i);
                if (slot.isEmpty()) {
                    inventory.setItem(i, stack.copy());
                    stack.setCount(0);
                    break;
                } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
                    int space = slot.getMaxStackSize() - slot.getCount();
                    int toAdd = Math.min(space, stack.getCount());
                    if (toAdd > 0) {
                        slot.grow(toAdd);
                        stack.shrink(toAdd);
                        if (stack.isEmpty()) break;
                    }
                }
            }
        }
    }

    public static class ShulkerBoxItemSource implements IItemSource {
        public final ItemStack shulkerStack;
        private final net.minecraft.network.chat.Component displayName;
        private final String storageId;

        public ShulkerBoxItemSource(ItemStack shulkerStack, net.minecraft.network.chat.Component displayName, String storageId) {
            this.shulkerStack = shulkerStack;
            this.displayName = displayName;
            this.storageId = storageId;
        }

        @Override
        public ItemStack extractItem(Ingredient ingredient, int amount, boolean simulate, Item lockedItem) {
            ItemContainerContents container = shulkerStack.get(DataComponents.CONTAINER);
            if (container == null) return ItemStack.EMPTY;

            List<ItemStack> stacks = new ArrayList<>();
            container.nonEmptyItemCopyStream().forEach(stacks::add);

            ItemStack extracted = ItemStack.EMPTY;
            boolean changed = false;

            for (int i = 0; i < stacks.size(); i++) {
                ItemStack stack = stacks.get(i);
                boolean isValid = lockedItem != null ? stack.is(lockedItem) : ingredient.test(stack);
                if (!stack.isEmpty() && isValid) {
                    int toTake = Math.min(stack.getCount(), amount - extracted.getCount());
                    if (toTake > 0) {
                        if (extracted.isEmpty()) {
                            extracted = stack.copyWithCount(toTake);
                        } else if (ItemStack.isSameItemSameComponents(extracted, stack)) {
                            extracted.grow(toTake);
                        } else {
                            continue;
                        }

                        if (!simulate) {
                            stack.shrink(toTake);
                            stacks.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
                            changed = true;
                        }

                        if (extracted.getCount() >= amount) break;
                    }
                }
            }

            if (changed && !simulate) {
                shulkerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
            }

            return extracted;
        }

        @Override
        public ItemStack extractOne(Ingredient ingredient, Item lockedItem) {
            net.minecraft.world.item.component.ItemContainerContents contents = shulkerStack.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY);
            java.util.List<net.minecraft.world.item.ItemStack> list = new java.util.ArrayList<>();
            contents.nonEmptyItemCopyStream().forEach(list::add); // Mutable copy of items

            net.minecraft.world.item.ItemStack extracted = net.minecraft.world.item.ItemStack.EMPTY;

            for (int i = 0; i < list.size(); i++) {
                net.minecraft.world.item.ItemStack stack = list.get(i);
                boolean isValid = lockedItem != null ? stack.is(lockedItem) : ingredient.test(stack);
                if (!stack.isEmpty() && isValid) {
                    extracted = stack.split(1); // split(1) modifies the original stack and returns exactly 1 item!
                    if (stack.isEmpty()) {
                        list.set(i, net.minecraft.world.item.ItemStack.EMPTY);
                    }
                    break;
                }
            }

            if (!extracted.isEmpty()) {
                shulkerStack.set(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(list));
            }
            return extracted; // Return the pulled item, NOT the remainder!
        }

        @Override
        public int countItem(Ingredient ingredient) {
            ItemContainerContents container = shulkerStack.get(DataComponents.CONTAINER);
            if (container == null) return 0;
            
            final int[] countHolder = new int[]{0};
            container.nonEmptyItemCopyStream().forEach(stack -> {
                if (ingredient.test(stack)) {
                    countHolder[0] += stack.getCount();
                }
            });
            return countHolder[0];
        }

        @Override
        public net.minecraft.network.chat.Component getDisplayName() {
            return displayName;
        }

        @Override
        public List<ItemStack> getAllItems() {
            List<ItemStack> items = new ArrayList<>(27);
            ItemContainerContents container = shulkerStack.get(DataComponents.CONTAINER);
            if (container != null) {
                container.nonEmptyItemCopyStream().forEach(stack -> items.add(stack));
            }
            // Ensure 27 slots for Shulker Box
            while(items.size() < 27) items.add(ItemStack.EMPTY);
            return items;
        }

        @Override
        public String getStorageId() {
            return storageId;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            List<ItemStack> items = getAllItems();
            if (slot >= 0 && slot < items.size()) return items.get(slot);
            return ItemStack.EMPTY;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            List<ItemStack> items = getAllItems();
            if (slot >= 0 && slot < 27) {
                while(items.size() <= slot) items.add(ItemStack.EMPTY);
                items.set(slot, stack);
                shulkerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            }
        }

        @Override
        public boolean isShulkerSource() {
            return true;
        }

        @Override
        public void insertItem(ItemStack stack) {
            ItemContainerContents contents = shulkerStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            List<ItemStack> list = new ArrayList<>();
            contents.nonEmptyItemCopyStream().forEach(list::add);
            for (int i = 0; i < 27; i++) {
                if (i >= list.size()) list.add(ItemStack.EMPTY);
                ItemStack slot = list.get(i);
                if (slot.isEmpty()) {
                    list.set(i, stack.copy());
                    stack.setCount(0);
                    break;
                } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
                    int space = slot.getMaxStackSize() - slot.getCount();
                    int toAdd = Math.min(space, stack.getCount());
                    if (toAdd > 0) {
                        slot.grow(toAdd);
                        stack.shrink(toAdd);
                        if (stack.isEmpty()) break;
                    }
                }
            }
            shulkerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
        }
    }

    public static List<IItemSource> getSources(Player player, int radius, boolean useEnderChest, boolean useShulkerBoxes) {
        List<IItemSource> storageSources = new ArrayList<>();
        
        if (useShulkerBoxes) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (isShulkerBox(stack)) {
                    net.minecraft.network.chat.Component displayName = stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) ? stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME) : stack.getHoverName();
                    storageSources.add(new ShulkerBoxItemSource(stack, displayName, "slot:" + i));
                }
            }
        }
        
        if (useEnderChest) {
            PlayerEnderChestContainer enderChest = player.getEnderChestInventory();
            storageSources.add(new InventorySource(enderChest, net.minecraft.network.chat.Component.literal("Ender Chest"), "Ender Chest"));
            
            if (useShulkerBoxes) {
                for (int i = 0; i < enderChest.getContainerSize(); i++) {
                    ItemStack stack = enderChest.getItem(i);
                    if (isShulkerBox(stack)) {
                    net.minecraft.network.chat.Component displayName = stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME) ? stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME) : stack.getHoverName();
                        storageSources.add(new ShulkerBoxItemSource(stack, displayName, "enderSlot:" + i));
                    }
                }
            }
        }
        
        if (radius > 0) {
            Level world = player.level();
            BlockPos playerPos = player.blockPosition();
            AABB searchBox = new AABB(playerPos).inflate(radius);
            
            BlockPos min = new BlockPos((int)searchBox.minX, (int)searchBox.minY, (int)searchBox.minZ);
            BlockPos max = new BlockPos((int)searchBox.maxX, (int)searchBox.maxY, (int)searchBox.maxZ);
            
            int foundChests = 0;
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity ||
                    be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity ||
                    be instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity) {
                    
                    if (com.radiuscrafting.RadiusCrafting.CONFIG.ignoreUnopenedLootChests) {
                        if (be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity lootable) {
                            if (lootable.getLootTable() != null) {
                                continue;
                            }
                        }
                    }
                    
                    if (be instanceof Container inv) {
                        net.minecraft.network.chat.Component displayName = be instanceof net.minecraft.world.Nameable nameable && nameable.hasCustomName() ? nameable.getCustomName() : world.getBlockState(pos).getBlock().getName();
                        String storageId = "block:" + pos.asLong();
                        storageSources.add(new InventorySource(inv, displayName, storageId));
                        foundChests++;
                    }
                }
            }
        }
        
        return storageSources;
    }

    private static boolean isShulkerBox(ItemStack stack) {
        return stack.has(DataComponents.CONTAINER);
    }

    public static void pullItemsToInventory(ServerPlayer player, net.minecraft.world.item.crafting.PlacementInfo placementInfo, boolean craftAll, int radius, boolean useEnderChest, boolean useShulkerBoxes) {
        List<IItemSource> sources = getSources(player, radius, useEnderChest, useShulkerBoxes);
        
        List<Ingredient> baseIngredients = placementInfo.ingredients();
        it.unimi.dsi.fastutil.ints.IntList slotsToIngredientIndex = placementInfo.slotsToIngredientIndex();
        
        // Build exactly 1 set of recipe ingredients
        List<Ingredient> ingredients = new java.util.ArrayList<>();
        for (int i = 0; i < slotsToIngredientIndex.size(); i++) {
            int idx = slotsToIngredientIndex.getInt(i);
            if (idx >= 0 && idx < baseIngredients.size()) {
                ingredients.add(baseIngredients.get(idx));
            }
        }
        
        // PHASE 1: The Virtual Ledger (Initialized once, mutated each iteration)
        java.util.Map<net.minecraft.world.item.Item, Integer> playerLedger = new java.util.HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && !stack.is(net.minecraft.world.item.Items.AIR)) {
                playerLedger.put(stack.getItem(), playerLedger.getOrDefault(stack.getItem(), 0) + stack.getCount());
            }
        }

        int craftIterations = 0;
        java.util.Map<Integer, net.minecraft.world.item.Item> lockedItems = new java.util.HashMap<>();
        
        while (true) {
            if (craftIterations >= 64) break;

            java.util.List<Integer> missingIndices = new java.util.ArrayList<>();

            for (int i = 0; i < ingredients.size(); i++) {
                net.minecraft.world.item.crafting.Ingredient ingredient = ingredients.get(i);
                // STRICT AIR/EMPTY CHECK
                if (ingredient == null || ingredient.isEmpty() || ingredient.test(net.minecraft.world.item.ItemStack.EMPTY)) {
                    continue;
                }

                boolean satisfied = false;
                for (net.minecraft.world.item.Item item : playerLedger.keySet()) {
                    if (playerLedger.get(item) > 0) {
                        boolean isValid = false;
                        if (lockedItems.containsKey(i)) {
                            isValid = item == lockedItems.get(i);
                        } else {
                            isValid = ingredient.test(new net.minecraft.world.item.ItemStack(item));
                        }
                        
                        if (isValid) {
                            playerLedger.put(item, playerLedger.get(item) - 1);
                            lockedItems.put(i, item);
                            satisfied = true;
                            break;
                        }
                    }
                }

                if (!satisfied) {
                    missingIndices.add(i);
                }
            }
            
            if (missingIndices.isEmpty()) {
                craftIterations++;
                if (!craftAll) break;
                continue;
            }

            // PHASE 2: Transactional Physical Extraction
            boolean success = true;
            class PendingExtraction {
                final IItemSource source;
                final ItemStack stack;
                PendingExtraction(IItemSource s, ItemStack st) { this.source = s; this.stack = st; }
            }
            List<PendingExtraction> pendingPulls = new ArrayList<>();
            
            for (int idx : missingIndices) {
                net.minecraft.world.item.crafting.Ingredient missing = ingredients.get(idx);
                net.minecraft.world.item.Item lockedItem = lockedItems.get(idx);
                
                boolean found = false;
                for (IItemSource source : sources) { 
                    net.minecraft.world.item.ItemStack extracted = source.extractOne(missing, lockedItem); 
                    if (extracted != null && !extracted.isEmpty() && !extracted.is(net.minecraft.world.item.Items.AIR)) {
                        pendingPulls.add(new PendingExtraction(source, extracted));
                        lockedItems.put(idx, extracted.getItem());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    success = false;
                    break;
                }
            }
            
            if (success) {
                // Commit to player inventory
                for (PendingExtraction pe : pendingPulls) {
                    player.getInventory().add(pe.stack);
                    // Add it to the ledger so the next iteration knows it is in the inventory
                    playerLedger.put(pe.stack.getItem(), playerLedger.getOrDefault(pe.stack.getItem(), 0) + 1);
                }
                craftIterations++;
                if (!craftAll) break;
            } else {
                // Abort and return to source to prevent partial flooding
                for (PendingExtraction pe : pendingPulls) {
                    pe.source.insertItem(pe.stack);
                    if (!pe.stack.isEmpty()) {
                        player.drop(pe.stack, false); // safety fallback
                    }
                }
                break;
            }
        }

        // PHASE 3: Sync to Client
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }

    public static List<ItemStack> getAllAvailableItems(Player player, int radius, boolean useEnderChest, boolean useShulkerBoxes) {
        List<ItemStack> allItems = new ArrayList<>();
        List<IItemSource> sources = getSources(player, radius, useEnderChest, useShulkerBoxes);
        for(IItemSource source : sources) {
            allItems.addAll(source.getAllItems());
        }
        return allItems;
    }

    public static void handleVirtualClick(ServerPlayer player, String storageId, int slotIndex) {
        List<IItemSource> sources = getSources(player, com.radiuscrafting.RadiusCrafting.CONFIG.searchRadius, com.radiuscrafting.RadiusCrafting.CONFIG.useEnderChest, com.radiuscrafting.RadiusCrafting.CONFIG.useShulkerBoxes);
        IItemSource targetSource = null;
        for (IItemSource source : sources) {
            if (source.getStorageId().equals(storageId)) {
                targetSource = source;
                break;
            }
        }
        
        if (targetSource == null) return;
        
        ItemStack cursorStack = player.containerMenu.getCarried();
        
        if (targetSource.isShulkerSource()) {
            if (net.minecraft.world.level.block.Block.byItem(cursorStack.getItem()) instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
                return;
            }
        }
        
        ItemStack slotStack = targetSource.getStackInSlot(slotIndex);
        
        if (cursorStack.isEmpty()) {
            player.containerMenu.setCarried(slotStack);
            targetSource.setStackInSlot(slotIndex, ItemStack.EMPTY);
        } else if (slotStack.isEmpty()) {
            targetSource.setStackInSlot(slotIndex, cursorStack);
            player.containerMenu.setCarried(ItemStack.EMPTY);
        } else {
            if (ItemStack.isSameItemSameComponents(cursorStack, slotStack)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                int toMove = Math.min(space, cursorStack.getCount());
                if (toMove > 0) {
                    slotStack.grow(toMove);
                    cursorStack.shrink(toMove);
                    targetSource.setStackInSlot(slotIndex, slotStack);
                }
            } else {
                targetSource.setStackInSlot(slotIndex, cursorStack);
                player.containerMenu.setCarried(slotStack);
            }
        }
        
        ItemStack newCursorStack = player.containerMenu.getCarried();
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
            -1, player.containerMenu.getStateId(), -1, newCursorStack
        ));
    }
    
    public static void handleRename(ServerPlayer player, String storageId, String newName) {
        if (storageId.startsWith("block:")) {
            try {
                long posLong = Long.parseLong(storageId.substring(6));
                BlockPos pos = BlockPos.of(posLong);
                Level world = player.level();
                BlockEntity be = world.getBlockEntity(pos);
                if (be != null) {
                    net.minecraft.core.component.DataComponentMap newComponents = net.minecraft.core.component.DataComponentMap.builder().set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(newName.trim())).build();
                    net.minecraft.core.component.DataComponentPatch patch = net.minecraft.core.component.DataComponentPatch.builder().set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(newName.trim())).build();
                    be.applyComponents(net.minecraft.core.component.DataComponentMap.EMPTY, patch);
                    be.setChanged();
                    net.minecraft.world.level.block.state.BlockState state = world.getBlockState(pos);
                    world.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                }
            } catch (Exception e) {}
        } else if (storageId.startsWith("slot:")) {
            try {
                int slot = Integer.parseInt(storageId.substring(5));
                ItemStack targetStack = player.getInventory().getItem(slot);
                if (targetStack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
                    targetStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(newName));
                    player.containerMenu.broadcastChanges();
                    player.inventoryMenu.broadcastChanges();
                }
            } catch (Exception e) {}
        } else if (storageId.startsWith("enderSlot:")) {
            try {
                int slot = Integer.parseInt(storageId.substring(10));
                ItemStack targetStack = player.getEnderChestInventory().getItem(slot);
                if (targetStack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem && blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
                    targetStack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(newName));
                }
            } catch (Exception e) {}
        }
        List<net.minecraft.world.item.ItemStack> allItems = getSources(player, com.radiuscrafting.RadiusCrafting.CONFIG.searchRadius, com.radiuscrafting.RadiusCrafting.CONFIG.useEnderChest, com.radiuscrafting.RadiusCrafting.CONFIG.useShulkerBoxes).stream().flatMap(source -> source.getAllItems().stream()).toList();
        com.radiuscrafting.network.ModMessages.sendSyncNearbyItems(player, allItems);
    }
}
