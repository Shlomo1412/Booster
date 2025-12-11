package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
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
 */
public class InfiniteCraftModule extends GUIModule {
    
    public static final String INFINITE_CRAFT_WIDGET_ID = "infinite_craft";
    
    // Settings
    private final ModuleSetting.NumberSetting delaySetting;
    
    private BoosterButton infiniteButton;
    private boolean isCrafting = false;
    private int craftDelay = 0;
    
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
            "Keeps crafting until materials run out\n" +
            "or your inventory is full.",
            button -> toggleInfiniteCraft(screen)
        );
        
        // Set editor info for dragging
        infiniteButton.setEditorInfo(this, INFINITE_CRAFT_WIDGET_ID, "Infinite Craft", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(infiniteButton);
        
        addDrawableChild.accept(infiniteButton);
    }
    
    /**
     * Toggles infinite crafting on/off.
     */
    private void toggleInfiniteCraft(HandledScreen<?> screen) {
        isCrafting = !isCrafting;
        updateButtonAppearance();
        
        if (isCrafting) {
            // Start crafting loop
            craftDelay = 0;
        }
    }
    
    /**
     * Updates the button appearance based on crafting state.
     */
    private void updateButtonAppearance() {
        if (infiniteButton != null) {
            // Visual feedback - could be enhanced with different text/color
            infiniteButton.setMessage(net.minecraft.text.Text.literal(isCrafting ? "■" : "∞"));
        }
    }
    
    /**
     * Called every tick to perform crafting operations.
     * Should be called from the screen's tick or render method.
     */
    public void tick(HandledScreen<?> screen) {
        if (!isCrafting || !isEnabled()) {
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
        
        // Apply delay
        if (craftDelay > 0) {
            craftDelay--;
            return;
        }
        
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
        
        // Set delay for next craft
        craftDelay = delaySetting.getValue();
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
        isCrafting = false;
        updateButtonAppearance();
    }
    
    /**
     * @return Whether infinite crafting is currently active
     */
    public boolean isCrafting() {
        return isCrafting;
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
