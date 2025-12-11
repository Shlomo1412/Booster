package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds an Infinite Craft button to the crafting table screen.
 * Continuously crafts the current recipe until materials run out or inventory is full.
 * 
 * Process:
 * 1. Detect the current crafting grid pattern (items in slots 1-9)
 * 2. Craft the output (shift-click slot 0)
 * 3. Refill the grid with the same materials from player inventory
 * 4. Repeat until materials run out
 */
public class InfiniteCraftModule extends GUIModule {
    
    public static final String INFINITE_CRAFT_WIDGET_ID = "infinite_craft";
    
    // Crafting states
    private enum CraftState {
        IDLE,           // Not crafting
        SAVING_PATTERN, // About to save the grid pattern
        CRAFTING,       // Performing the craft (shift-click output)
        REFILLING,      // Refilling the grid with materials
        WAITING         // Waiting between operations
    }
    
    // Settings
    private final ModuleSetting.NumberSetting delaySetting;
    
    private BoosterButton infiniteButton;
    private CraftState craftState = CraftState.IDLE;
    private int tickDelay = 0;
    
    // Saved pattern: stores the Item for each slot (null if empty)
    // Slots 1-9 in CraftingScreenHandler are the 3x3 grid
    private Item[] savedPattern = new Item[9];
    private int currentRefillSlot = 0;  // Which grid slot we're currently refilling (0-8)
    
    public InfiniteCraftModule() {
        super(
            "infinite_craft",
            "Infinite Craft",
            "Adds a button to craft repeatedly.\n" +
            "Keeps crafting until materials run out\n" +
            "or your inventory is full.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Initialize settings
        this.delaySetting = new ModuleSetting.NumberSetting(
            "delay",
            "Craft Delay",
            "Delay between crafts in ticks (20 = 1 second)",
            2,  // Default: 2 ticks (0.1 seconds)
            0,  // Min
            20  // Max
        );
        
        registerSetting(delaySetting);
    }
    
    /**
     * Creates the infinite craft button for the crafting screen.
     *
     * @param screen The screen to add the button to
     * @param anchorX The anchor X position (right edge of crafting GUI)
     * @param anchorY The anchor Y position (top of crafting GUI)
     * @param addDrawableChild Callback to add the button
     */
    public void createButton(HandledScreen<?> screen, int anchorX, int anchorY,
                             Consumer<BoosterButton> addDrawableChild) {
        // Get per-widget settings - positioned below Clear Grid button
        WidgetSettings settings = getWidgetSettings(INFINITE_CRAFT_WIDGET_ID, 4, 22);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        infiniteButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "∞",  // Infinity icon
            "Infinite Craft",
            "Click to toggle continuous crafting.\n" +
            "Saves the current grid pattern, crafts,\n" +
            "then refills the grid with the same items.\n" +
            "Repeats until materials run out.",
            button -> toggleInfiniteCraft(screen)
        );
        
        // Apply display mode from settings
        infiniteButton.setDisplayMode(settings.getDisplayMode());
        
        // Set editor info for dragging
        infiniteButton.setEditorInfo(this, INFINITE_CRAFT_WIDGET_ID, "Infinite Craft", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(infiniteButton);
        
        addDrawableChild.accept(infiniteButton);
    }
    
    /**
     * Toggles infinite crafting on/off.
     */
    private void toggleInfiniteCraft(HandledScreen<?> screen) {
        if (craftState == CraftState.IDLE) {
            // Start crafting - first save the pattern
            craftState = CraftState.SAVING_PATTERN;
            tickDelay = 0;
        } else {
            // Stop crafting
            stopCrafting();
        }
        updateButtonAppearance();
    }
    
    /**
     * Updates the button appearance based on crafting state.
     */
    private void updateButtonAppearance() {
        if (infiniteButton != null) {
            // Visual feedback - stop icon when crafting, infinity when idle
            infiniteButton.setMessage(net.minecraft.text.Text.literal(
                craftState != CraftState.IDLE ? "■" : "∞"
            ));
        }
    }
    
    /**
     * Called every tick to perform crafting operations.
     * Should be called from the screen's tick or render method.
     */
    public void tick(HandledScreen<?> screen) {
        if (craftState == CraftState.IDLE || !isEnabled()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) {
            stopCrafting();
            return;
        }
        
        if (!(screen.getScreenHandler() instanceof CraftingScreenHandler craftingHandler)) {
            stopCrafting();
            return;
        }
        
        // Apply delay between operations
        if (tickDelay > 0) {
            tickDelay--;
            return;
        }
        
        switch (craftState) {
            case SAVING_PATTERN -> {
                saveGridPattern(craftingHandler);
                // Check if there's actually a recipe
                if (!craftingHandler.getSlot(0).hasStack()) {
                    stopCrafting();
                    return;
                }
                craftState = CraftState.CRAFTING;
                tickDelay = 1;
            }
            
            case CRAFTING -> {
                // Check if there's a valid recipe output
                Slot outputSlot = craftingHandler.getSlot(0);
                if (!outputSlot.hasStack()) {
                    // No recipe result - stop crafting
                    stopCrafting();
                    return;
                }
                
                // Check if player inventory has space
                if (!hasInventorySpace(client, outputSlot.getStack())) {
                    stopCrafting();
                    return;
                }
                
                // Perform the craft (shift-click output slot)
                client.interactionManager.clickSlot(
                    craftingHandler.syncId,
                    0,  // Output slot
                    0,
                    SlotActionType.QUICK_MOVE,
                    client.player
                );
                
                // Now transition to refilling
                craftState = CraftState.REFILLING;
                currentRefillSlot = 0;
                tickDelay = 1;
            }
            
            case REFILLING -> {
                // Refill one slot at a time
                if (!refillNextSlot(client, craftingHandler)) {
                    // Failed to refill - no more materials
                    stopCrafting();
                    return;
                }
                
                currentRefillSlot++;
                if (currentRefillSlot >= 9) {
                    // All slots refilled - back to crafting
                    craftState = CraftState.WAITING;
                    tickDelay = delaySetting.getValue();
                } else {
                    tickDelay = 1;  // Small delay between slot refills
                }
            }
            
            case WAITING -> {
                // Wait completed, check if there's still a valid recipe
                if (craftingHandler.getSlot(0).hasStack()) {
                    craftState = CraftState.CRAFTING;
                } else {
                    stopCrafting();
                }
            }
            
            default -> stopCrafting();
        }
    }
    
    /**
     * Saves the current crafting grid pattern.
     * Stores which item type is in each slot (1-9).
     */
    private void saveGridPattern(CraftingScreenHandler handler) {
        for (int i = 0; i < 9; i++) {
            Slot slot = handler.getSlot(i + 1);  // Slots 1-9 are the crafting grid
            if (slot.hasStack()) {
                savedPattern[i] = slot.getStack().getItem();
            } else {
                savedPattern[i] = null;
            }
        }
    }
    
    /**
     * Attempts to refill the next slot that needs an item.
     * @return true if slot was empty or successfully refilled, false if couldn't find item
     */
    private boolean refillNextSlot(MinecraftClient client, CraftingScreenHandler handler) {
        // Get the item that should be in this slot
        Item neededItem = savedPattern[currentRefillSlot];
        
        // If no item needed in this slot, skip it
        if (neededItem == null) {
            return true;
        }
        
        // Check if slot already has the item
        Slot gridSlot = handler.getSlot(currentRefillSlot + 1);  // Slots 1-9
        if (gridSlot.hasStack() && gridSlot.getStack().getItem() == neededItem) {
            return true;  // Already has the right item
        }
        
        // Find the item in player inventory and move it to the crafting grid
        // Player inventory slots in CraftingScreenHandler: 10-36 (main inventory) and 37-45 (hotbar)
        // Actually: crafting output=0, grid=1-9, player inv=10-36, hotbar=37-45
        
        // Search player inventory for the item
        for (int invSlot = 10; invSlot <= 45; invSlot++) {
            Slot slot = handler.getSlot(invSlot);
            if (slot.hasStack() && slot.getStack().getItem() == neededItem) {
                // Found the item! Move it to the crafting grid
                // Pick up the stack
                client.interactionManager.clickSlot(
                    handler.syncId,
                    invSlot,
                    0,
                    SlotActionType.PICKUP,
                    client.player
                );
                
                // Place one item in the grid slot (right-click to place one)
                client.interactionManager.clickSlot(
                    handler.syncId,
                    currentRefillSlot + 1,  // Grid slots 1-9
                    1,  // Right-click to place one
                    SlotActionType.PICKUP,
                    client.player
                );
                
                // Put remaining items back
                if (client.player.currentScreenHandler.getCursorStack() != null && 
                    !client.player.currentScreenHandler.getCursorStack().isEmpty()) {
                    client.interactionManager.clickSlot(
                        handler.syncId,
                        invSlot,
                        0,
                        SlotActionType.PICKUP,
                        client.player
                    );
                }
                
                return true;
            }
        }
        
        // Couldn't find the item
        return false;
    }
    
    /**
     * Checks if the player's inventory has space for the given item.
     */
    private boolean hasInventorySpace(MinecraftClient client, ItemStack craftResult) {
        if (client.player == null) return false;
        
        PlayerInventory inventory = client.player.getInventory();
        
        // Check each main inventory slot (9-35) and hotbar (0-8)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            
            // Empty slot available
            if (stack.isEmpty()) {
                return true;
            }
            
            // Can stack with existing item
            if (ItemStack.areItemsAndComponentsEqual(stack, craftResult) && 
                stack.getCount() < stack.getMaxCount()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Stops the infinite crafting process.
     */
    public void stopCrafting() {
        craftState = CraftState.IDLE;
        tickDelay = 0;
        currentRefillSlot = 0;
        savedPattern = new Item[9];
        updateButtonAppearance();
    }
    
    /**
     * @return Whether infinite crafting is currently active
     */
    public boolean isCrafting() {
        return craftState != CraftState.IDLE;
    }
    
    /**
     * Gets the infinite button.
     */
    public BoosterButton getInfiniteButton() {
        return infiniteButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        stopCrafting();
        infiniteButton = null;
    }
}
