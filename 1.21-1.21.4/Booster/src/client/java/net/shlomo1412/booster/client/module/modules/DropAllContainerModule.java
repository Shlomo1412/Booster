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
 * Module that adds a Drop All (Container) button to container screens.
 * Drops all items from the container (not the player's inventory).
 */
public class DropAllContainerModule extends GUIModule {
    
    // Widget ID for per-widget settings
    public static final String DROP_CONTAINER_WIDGET_ID = "drop_all_container_items";
    
    // Settings
    private final ModuleSetting.BooleanSetting confirmDropSetting;
    
    // Runtime state
    private BoosterButton dropContainerButton;
    
    public DropAllContainerModule() {
        super(
            "drop_all_container",
            "Drop All (Container)",
            "Adds a button to drop all items from the container.\n" +
            "Only appears on container screens.\n" +
            "Hold SHIFT to bypass confirmation.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
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
     * Creates the drop all container button.
     *
     * @param screen The screen to add the button to
     * @param anchorX The anchor X position (right edge of container)
     * @param anchorY The anchor Y position (top of container)
     * @param addDrawableChild Callback to add the button
     */
    public void createButton(HandledScreen<?> screen, int anchorX, int anchorY,
                             Consumer<BoosterButton> addDrawableChild) {
        
        // Get per-widget settings
        WidgetSettings settings = getWidgetSettings(DROP_CONTAINER_WIDGET_ID, 4, 44);  // Below steal/store
        
        // Calculate button position
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        String tooltip = confirmDropSetting.getValue() 
            ? "Drop all items from the container.\nHold SHIFT and click to drop."
            : "Drop all items from the container.";
        
        dropContainerButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🗑",  // Trash icon
            "Drop Container",
            tooltip,
            button -> dropContainerItems(screen)
        );
        
        // Apply display mode from settings
        dropContainerButton.setDisplayMode(settings.getDisplayMode());
        
        // Set editor info
        dropContainerButton.setEditorInfo(this, DROP_CONTAINER_WIDGET_ID, "Drop Container", anchorX, anchorY);
        
        addDrawableChild.accept(dropContainerButton);
    }
    
    /**
     * Gets the buttons created by this module.
     */
    public List<BoosterButton> getButtons() {
        List<BoosterButton> buttons = new ArrayList<>();
        if (dropContainerButton != null) buttons.add(dropContainerButton);
        return buttons;
    }
    
    /**
     * Drops all items from the container.
     */
    private void dropContainerItems(HandledScreen<?> screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        // Check if SHIFT is required and held
        if (confirmDropSetting.getValue() && !net.minecraft.client.gui.screen.Screen.hasShiftDown()) {
            // Don't drop - SHIFT not held
            return;
        }
        
        var handler = screen.getScreenHandler();
        
        for (Slot slot : handler.slots) {
            // Skip player inventory slots - we only want container slots
            if (slot.inventory instanceof PlayerInventory) continue;
            
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
        ids.add(DROP_CONTAINER_WIDGET_ID);
        return ids;
    }
    
    @Override
    protected void onEnable() {
        // Nothing special to do on enable
    }
    
    @Override
    protected void onDisable() {
        dropContainerButton = null;
    }
}
