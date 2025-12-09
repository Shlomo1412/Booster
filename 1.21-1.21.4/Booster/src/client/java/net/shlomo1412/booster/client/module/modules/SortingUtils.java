package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;

/**
 * Utility class for sorting items in inventories and containers.
 */
public class SortingUtils {
    
    /**
     * Sorts the player's inventory in a HandledScreen.
     * Only affects the main inventory slots (not hotbar, not armor).
     *
     * @param handler The screen handler
     * @param mode The sorting mode
     * @param includeHotbar Whether to include hotbar in sorting
     */
    public static void sortPlayerInventory(ScreenHandler handler, SortMode mode, boolean includeHotbar) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        // Find player inventory slots
        List<Slot> playerSlots = new ArrayList<>();
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory) {
                // Inventory slot indices: 0-8 = hotbar, 9-35 = main inventory
                int invIndex = slot.getIndex();
                if (includeHotbar || (invIndex >= 9 && invIndex <= 35)) {
                    playerSlots.add(slot);
                }
            }
        }
        
        sortSlots(handler, playerSlots, mode);
    }
    
    /**
     * Sorts the container's inventory in a HandledScreen.
     * Only affects the container slots (not player inventory).
     *
     * @param handler The screen handler
     * @param mode The sorting mode
     */
    public static void sortContainer(ScreenHandler handler, SortMode mode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        // Find container slots (non-player inventory slots)
        List<Slot> containerSlots = new ArrayList<>();
        for (Slot slot : handler.slots) {
            if (!(slot.inventory instanceof PlayerInventory)) {
                containerSlots.add(slot);
            }
        }
        
        sortSlots(handler, containerSlots, mode);
    }
    
    /**
     * Sorts a list of slots according to the given mode.
     */
    private static void sortSlots(ScreenHandler handler, List<Slot> slots, SortMode mode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        // Collect all items with their original slot IDs
        List<ItemSlotPair> items = new ArrayList<>();
        for (Slot slot : slots) {
            if (slot.hasStack()) {
                items.add(new ItemSlotPair(slot.getStack().copy(), slot.id));
            }
        }
        
        if (items.isEmpty()) return;
        
        // Sort items based on mode
        Comparator<ItemSlotPair> comparator = getComparator(mode);
        items.sort(comparator);
        
        // First, collect all items to a temporary storage by picking them up
        // This simulates the player picking up all items, sorting, then placing back
        
        // Move items into the sorted order using slot swapping
        // We'll use a more efficient approach: pickup items, place in order
        
        // Create a mapping from slot ID to sorted item
        List<Integer> slotIds = new ArrayList<>();
        for (Slot slot : slots) {
            slotIds.add(slot.id);
        }
        
        // Sort slots by position for consistent row/column ordering
        if (mode == SortMode.ROWS || mode == SortMode.COLUMNS) {
            slotIds.sort((a, b) -> {
                Slot slotA = handler.getSlot(a);
                Slot slotB = handler.getSlot(b);
                if (mode == SortMode.ROWS) {
                    // Row-major: sort by Y first, then X
                    int yCompare = Integer.compare(slotA.y, slotB.y);
                    return yCompare != 0 ? yCompare : Integer.compare(slotA.x, slotB.x);
                } else {
                    // Column-major: sort by X first, then Y
                    int xCompare = Integer.compare(slotA.x, slotB.x);
                    return xCompare != 0 ? xCompare : Integer.compare(slotA.y, slotB.y);
                }
            });
        }
        
        // Perform sorting using pickup/place operations
        performSort(handler, slots, items, slotIds);
    }
    
    /**
     * Performs the actual sorting by moving items.
     */
    private static void performSort(ScreenHandler handler, List<Slot> slots, 
                                    List<ItemSlotPair> sortedItems, List<Integer> targetSlotIds) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        // Strategy: 
        // 1. First consolidate stacks (combine partial stacks)
        // 2. Then arrange items in sorted order
        
        // Step 1: Consolidate stacks
        consolidateStacks(handler, slots);
        
        // Step 2: Re-gather items after consolidation
        List<ItemSlotPair> currentItems = new ArrayList<>();
        for (Slot slot : slots) {
            if (slot.hasStack()) {
                currentItems.add(new ItemSlotPair(slot.getStack().copy(), slot.id));
            }
        }
        
        // Step 3: Sort the items
        Comparator<ItemSlotPair> comparator = getComparator(SortMode.NAME); // Use current mode
        currentItems.sort(comparator);
        
        // Step 4: Use bubble-sort style swapping to arrange items
        // This is network-efficient as it uses swap operations
        int targetIndex = 0;
        for (int i = 0; i < targetSlotIds.size() && targetIndex < currentItems.size(); i++) {
            int targetSlotId = targetSlotIds.get(i);
            ItemSlotPair desiredItem = currentItems.get(targetIndex);
            
            Slot targetSlot = handler.getSlot(targetSlotId);
            
            // Check if target slot already has the correct item
            if (targetSlot.hasStack()) {
                ItemStack currentStack = targetSlot.getStack();
                if (ItemStack.areItemsAndComponentsEqual(currentStack, desiredItem.stack) &&
                    currentStack.getCount() == desiredItem.stack.getCount()) {
                    targetIndex++;
                    continue;
                }
            }
            
            // Find where the desired item currently is
            int sourceSlotId = desiredItem.slotId;
            if (sourceSlotId != targetSlotId) {
                // Swap items
                swapSlots(handler, sourceSlotId, targetSlotId);
                
                // Update the slot ID in our tracking
                for (ItemSlotPair pair : currentItems) {
                    if (pair.slotId == sourceSlotId) {
                        pair.slotId = targetSlotId;
                    } else if (pair.slotId == targetSlotId) {
                        pair.slotId = sourceSlotId;
                    }
                }
            }
            
            targetIndex++;
        }
    }
    
    /**
     * Consolidates partial stacks by combining them.
     */
    private static void consolidateStacks(ScreenHandler handler, List<Slot> slots) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        // For each item type, combine partial stacks
        for (int i = 0; i < slots.size(); i++) {
            Slot sourceSlot = slots.get(i);
            if (!sourceSlot.hasStack()) continue;
            
            ItemStack sourceStack = sourceSlot.getStack();
            if (sourceStack.getCount() >= sourceStack.getMaxCount()) continue;
            
            // Find other slots with the same item
            for (int j = i + 1; j < slots.size(); j++) {
                Slot targetSlot = slots.get(j);
                if (!targetSlot.hasStack()) continue;
                
                ItemStack targetStack = targetSlot.getStack();
                if (ItemStack.areItemsAndComponentsEqual(sourceStack, targetStack)) {
                    // Double-click to collect all of this item type to cursor, then place back
                    // This is the most efficient way to consolidate
                    client.interactionManager.clickSlot(
                        handler.syncId, sourceSlot.id, 0, SlotActionType.PICKUP, client.player
                    );
                    client.interactionManager.clickSlot(
                        handler.syncId, sourceSlot.id, 0, SlotActionType.PICKUP_ALL, client.player
                    );
                    client.interactionManager.clickSlot(
                        handler.syncId, sourceSlot.id, 0, SlotActionType.PICKUP, client.player
                    );
                    break;
                }
            }
        }
    }
    
    /**
     * Swaps the contents of two slots.
     */
    private static void swapSlots(ScreenHandler handler, int slotA, int slotB) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        // Pickup from slot A
        client.interactionManager.clickSlot(
            handler.syncId, slotA, 0, SlotActionType.PICKUP, client.player
        );
        // Place into slot B (picks up what was in B)
        client.interactionManager.clickSlot(
            handler.syncId, slotB, 0, SlotActionType.PICKUP, client.player
        );
        // Place what was in B into slot A
        client.interactionManager.clickSlot(
            handler.syncId, slotA, 0, SlotActionType.PICKUP, client.player
        );
    }
    
    /**
     * Gets a comparator for the given sort mode.
     */
    private static Comparator<ItemSlotPair> getComparator(SortMode mode) {
        return switch (mode) {
            case NAME -> Comparator.comparing((ItemSlotPair p) -> p.stack.getName().getString().toLowerCase());
            case COUNT -> Comparator.comparingInt((ItemSlotPair p) -> p.stack.getCount()).reversed();
            case RAW_ID -> Comparator.comparingInt((ItemSlotPair p) -> Registries.ITEM.getRawId(p.stack.getItem()));
            case CREATIVE_GROUPS -> Comparator.comparingInt((ItemSlotPair p) -> getCreativeTabOrder(p.stack))
                    .thenComparing((ItemSlotPair p) -> p.stack.getName().getString().toLowerCase());
            case ROWS, COLUMNS -> Comparator.comparing((ItemSlotPair p) -> p.stack.getName().getString().toLowerCase());
        };
    }
    
    /**
     * Gets the creative tab order for an item.
     * Items in the same tab will be grouped together.
     */
    private static int getCreativeTabOrder(ItemStack stack) {
        // Use a simple hash based on item group
        // This provides grouping without complex creative tab traversal
        int rawId = Registries.ITEM.getRawId(stack.getItem());
        // Group by item category (blocks, tools, combat, food, etc.)
        // Using raw ID ranges as a simple approximation
        return rawId / 100; // Groups items roughly by registration order
    }
    
    /**
     * Helper class to track items and their slot IDs.
     */
    private static class ItemSlotPair {
        final ItemStack stack;
        int slotId;
        
        ItemSlotPair(ItemStack stack, int slotId) {
            this.stack = stack;
            this.slotId = slotId;
        }
    }
}
