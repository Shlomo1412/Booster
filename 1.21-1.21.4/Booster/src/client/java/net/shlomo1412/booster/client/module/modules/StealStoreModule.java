package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Module that adds Steal and Store buttons to container screens.
 * - Steal (⬇): Moves all items from the container to the player's inventory
 * - Store (⬆): Moves all items from the player's inventory to the container
 */
public class StealStoreModule extends GUIModule {
    
    // Widget IDs for per-widget settings
    public static final String STORE_WIDGET_ID = "store";
    public static final String STEAL_WIDGET_ID = "steal";
    
    private BoosterButton stealButton;
    private BoosterButton storeButton;
    
    public StealStoreModule() {
        super(
            "steal_store",
            "Steal/Store",
            "Adds buttons to quickly move items between container and inventory.\n" +
            "⬇ Steal: Move all items from container to your inventory.\n" +
            "⬆ Store: Move all items from your inventory to container.",
            true,
            20,  // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Creates the Steal and Store buttons for a container screen.
     *
     * @param screen           The screen to add buttons to
     * @param anchorX          The anchor X position (right edge of container)
     * @param anchorY          The anchor Y position (top of container)
     * @param addDrawableChild Callback to add the button to the screen
     */
    public void createButtons(HandledScreen<?> screen, int anchorX, int anchorY, 
                              Consumer<BoosterButton> addDrawableChild) {
        // Get per-widget settings (creates with defaults if not exists)
        WidgetSettings storeSettings = getWidgetSettings(STORE_WIDGET_ID, 4, 0);  // Default: 4px right, top aligned
        WidgetSettings stealSettings = getWidgetSettings(STEAL_WIDGET_ID, 4, 22); // Default: 4px right, below store
        
        // Store button (⬆) - on top
        int storeX = anchorX + storeSettings.getOffsetX();
        int storeY = anchorY + storeSettings.getOffsetY();
        storeButton = new BoosterButton(
            storeX, storeY,
            storeSettings.getWidth(), storeSettings.getHeight(),
            "⬆",
            "Store",
            "Move all items from your inventory to the container",
            button -> storeItems(screen)
        );
        storeButton.setDisplayMode(storeSettings.getDisplayMode());
        storeButton.setEditorInfo(this, STORE_WIDGET_ID, "Store", anchorX, anchorY);
        
        // Steal button (⬇) - below store
        int stealX = anchorX + stealSettings.getOffsetX();
        int stealY = anchorY + stealSettings.getOffsetY();
        stealButton = new BoosterButton(
            stealX, stealY,
            stealSettings.getWidth(), stealSettings.getHeight(),
            "⬇",
            "Steal",
            "Move all items from the container to your inventory",
            button -> stealItems(screen)
        );
        stealButton.setDisplayMode(stealSettings.getDisplayMode());
        stealButton.setEditorInfo(this, STEAL_WIDGET_ID, "Steal", anchorX, anchorY);
        
        addDrawableChild.accept(storeButton);
        addDrawableChild.accept(stealButton);
    }
    
    /**
     * Gets the buttons created by this module.
     * 
     * @return List of buttons, or empty if not created
     */
    public List<BoosterButton> getButtons() {
        List<BoosterButton> buttons = new ArrayList<>();
        if (stealButton != null) buttons.add(stealButton);
        if (storeButton != null) buttons.add(storeButton);
        return buttons;
    }
    
    /**
     * Steals all items from the container to player inventory.
     */
    private void stealItems(HandledScreen<?> screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        var handler = screen.getScreenHandler();
        
        for (Slot slot : handler.slots) {
            // Skip player inventory slots
            if (slot.inventory instanceof PlayerInventory) continue;
            
            if (slot.hasStack()) {
                // Shift-click to move to player inventory
                client.interactionManager.clickSlot(
                    handler.syncId,
                    slot.id,
                    0,
                    SlotActionType.QUICK_MOVE,
                    client.player
                );
            }
        }
    }
    
    /**
     * Stores all items from player inventory to the container.
     */
    private void storeItems(HandledScreen<?> screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        var handler = screen.getScreenHandler();
        
        for (Slot slot : handler.slots) {
            // Only process player inventory slots (not hotbar for now)
            if (!(slot.inventory instanceof PlayerInventory)) continue;
            
            if (slot.hasStack()) {
                // Shift-click to move to container
                client.interactionManager.clickSlot(
                    handler.syncId,
                    slot.id,
                    0,
                    SlotActionType.QUICK_MOVE,
                    client.player
                );
            }
        }
    }
    
    @Override
    protected void onEnable() {
        // Nothing special to do on enable
    }
    
    @Override
    protected void onDisable() {
        // Clear button references
        stealButton = null;
        storeButton = null;
    }
}
