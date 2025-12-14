package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Module that adds a Drop All button to inventory and container screens.
 * Drops all items from the player's inventory (main slots, optionally hotbar).
 * Has separate positions for inventory screen vs container screen.
 */
public class DropAllModule extends GUIModule {
    
    // Widget IDs for per-widget settings - separate for inventory vs container screens
    public static final String DROP_ALL_WIDGET_ID = "drop_all";                    // On inventory screen
    public static final String DROP_ALL_CONTAINER_WIDGET_ID = "drop_all_container"; // On container screen
    
    // Settings
    private final ModuleSetting.BooleanSetting includeHotbarSetting;
    private final ModuleSetting.BooleanSetting confirmDropSetting;
    
    // Runtime state
    private BoosterButton dropAllButton;
    private boolean onContainerScreen = false;
    
    public DropAllModule() {
        super(
            "drop_all",
            "Drop All",
            "Adds a button to drop all items from your inventory.\n" +
            "Works on both inventory and container screens.\n" +
            "Hold SHIFT to bypass confirmation.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Include hotbar setting
        this.includeHotbarSetting = new ModuleSetting.BooleanSetting(
            "include_hotbar",
            "Include Hotbar",
            "Whether to include the hotbar when dropping items",
            false
        );
        registerSetting(includeHotbarSetting);
        
        // Confirm drop setting
        this.confirmDropSetting = new ModuleSetting.BooleanSetting(
            "confirm_drop",
            "Require SHIFT",
            "Require holding SHIFT to drop items (prevents accidental drops)",
            true
        );
        registerSetting(confirmDropSetting);
    }
    
    /**
     * Creates the drop all button for a screen.
     *
     * @param screen The screen to add the button to
     * @param anchorX The anchor X position (right edge of container)
     * @param anchorY The anchor Y position (top of container)
     * @param backgroundHeight The height of the container background
     * @param isContainerScreen Whether this is a container screen (vs player inventory)
     * @param addDrawableChild Callback to add the button
     */
    public void createButton(HandledScreen<?> screen, int anchorX, int anchorY, int backgroundHeight,
                             boolean isContainerScreen, Consumer<BoosterButton> addDrawableChild) {
        this.onContainerScreen = isContainerScreen;
        
        // Use different widget ID based on screen type
        String widgetId = isContainerScreen ? DROP_ALL_CONTAINER_WIDGET_ID : DROP_ALL_WIDGET_ID;
        
        // Calculate the base Y offset to position at the inventory section line
        // For containers: inventory section is at backgroundHeight - 83 from container top
        // For player inventory: position at top (offset 0)
        int inventorySectionOffset = isContainerScreen ? (backgroundHeight - 83) : 0;
        
        // Get per-widget settings - default positions
        int defaultOffsetX = isContainerScreen ? 4 : -24;
        int defaultOffsetY = isContainerScreen ? 22 : 22;  // Below sort button
        
        WidgetSettings settings = getWidgetSettings(widgetId, defaultOffsetX, defaultOffsetY);
        
        // Calculate button position
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + inventorySectionOffset + settings.getOffsetY();
        
        String tooltip = confirmDropSetting.getValue() 
            ? "Drop all items from your inventory.\nHold SHIFT and click to drop."
            : "Drop all items from your inventory.";
        
        dropAllButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🗑",  // Trash icon
            "Drop All",
            tooltip,
            button -> dropAllItems(screen)
        );
        
        // Apply display mode from settings
        dropAllButton.setDisplayMode(settings.getDisplayMode());
        
        // Set editor info
        int editorAnchorY = anchorY + inventorySectionOffset;
        dropAllButton.setEditorInfo(this, widgetId, "Drop All", anchorX, editorAnchorY);
        
        addDrawableChild.accept(dropAllButton);
    }
    
    /**
     * Gets the buttons created by this module.
     */
    public List<BoosterButton> getButtons() {
        List<BoosterButton> buttons = new ArrayList<>();
        if (dropAllButton != null) buttons.add(dropAllButton);
        return buttons;
    }
    
    /**
     * Drops all items from the player's inventory.
     */
    private void dropAllItems(HandledScreen<?> screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        // Check if SHIFT is required and held
        if (confirmDropSetting.getValue() && !net.minecraft.client.gui.screen.Screen.hasShiftDown()) {
            // Don't drop - SHIFT not held
            return;
        }
        
        var handler = screen.getScreenHandler();
        boolean includeHotbar = includeHotbarSetting.getValue();
        
        for (Slot slot : handler.slots) {
            // Only process player inventory slots
            if (!(slot.inventory instanceof PlayerInventory)) continue;
            
            // PlayerInventory slot indices:
            // 0-8: Hotbar
            // 9-35: Main inventory
            // 36-39: Armor
            // 40: Offhand
            int inventoryIndex = slot.getIndex();
            
            // Skip armor and offhand
            if (inventoryIndex >= 36) continue;
            
            // Skip hotbar if not included
            if (!includeHotbar && inventoryIndex < 9) continue;
            
            if (slot.hasStack()) {
                // CTRL+Q to drop entire stack
                client.interactionManager.clickSlot(
                    handler.syncId,
                    slot.id,
                    1,  // Button 1 with CTRL = drop stack
                    SlotActionType.THROW,
                    client.player
                );
            }
        }
    }
    
    @Override
    public Set<String> getWidgetIds() {
        Set<String> ids = new HashSet<>();
        ids.add(DROP_ALL_WIDGET_ID);
        ids.add(DROP_ALL_CONTAINER_WIDGET_ID);
        return ids;
    }
    
    @Override
    protected void onEnable() {
        // Nothing special to do on enable
    }
    
    @Override
    protected void onDisable() {
        dropAllButton = null;
    }
}
