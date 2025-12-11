package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds a Clear Grid button to the crafting table screen.
 * Clears all items from the crafting grid back to the player's inventory.
 */
public class ClearGridModule extends GUIModule {
    
    public static final String CLEAR_GRID_WIDGET_ID = "clear_grid";
    
    private BoosterButton clearButton;
    
    public ClearGridModule() {
        super(
            "clear_grid",
            "Clear Grid",
            "Adds a button to clear the crafting grid.\n" +
            "Moves all items from the grid back to your inventory.",
            true,
            20,  // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Creates the clear grid button for the crafting screen.
     *
     * @param screen The screen to add the button to
     * @param anchorX The anchor X position (right edge of crafting GUI)
     * @param anchorY The anchor Y position (top of crafting GUI)
     * @param addDrawableChild Callback to add the button
     */
    public void createButton(HandledScreen<?> screen, int anchorX, int anchorY,
                             Consumer<BoosterButton> addDrawableChild) {
        // Get per-widget settings
        WidgetSettings settings = getWidgetSettings(CLEAR_GRID_WIDGET_ID, 4, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        clearButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "✕",  // Clear icon
            "Clear Grid",
            "Clears all items from the crafting grid\nback to your inventory.",
            button -> clearGrid(screen)
        );
        
        // Set editor info for dragging
        clearButton.setEditorInfo(this, CLEAR_GRID_WIDGET_ID, "Clear Grid", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(clearButton);
        
        addDrawableChild.accept(clearButton);
    }
    
    /**
     * Clears all items from the crafting grid.
     */
    private void clearGrid(HandledScreen<?> screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        if (!(screen.getScreenHandler() instanceof CraftingScreenHandler craftingHandler)) {
            return;
        }
        
        // Crafting grid slots are 1-9 (slot 0 is output)
        // Shift-click each to move to inventory
        for (int i = 1; i <= 9; i++) {
            Slot slot = craftingHandler.getSlot(i);
            if (slot.hasStack()) {
                // Shift-click to move to inventory
                client.interactionManager.clickSlot(
                    craftingHandler.syncId,
                    i,
                    0,
                    SlotActionType.QUICK_MOVE,
                    client.player
                );
            }
        }
    }
    
    /**
     * Gets the clear button.
     */
    public BoosterButton getClearButton() {
        return clearButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        clearButton = null;
    }
}
