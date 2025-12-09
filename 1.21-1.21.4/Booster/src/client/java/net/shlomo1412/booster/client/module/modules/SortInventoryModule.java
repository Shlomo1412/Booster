package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.SortButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Module that adds a Sort Inventory button to inventory and container screens.
 * Has separate customizable positions for inventory screen vs container screen.
 * - Sorts the player's inventory (main slots, optionally hotbar)
 * - Alt+Scroll to change sort mode
 */
public class SortInventoryModule extends GUIModule {
    
    // Widget IDs for per-widget settings - separate for inventory vs container screens
    public static final String SORT_INV_WIDGET_ID = "sort_inventory";         // On inventory screen
    public static final String SORT_INV_CONTAINER_WIDGET_ID = "sort_inventory_container";  // On container screen
    
    // Settings
    private final ModuleSetting.EnumSetting<SortMode> sortModeSetting;
    private final ModuleSetting.BooleanSetting includeHotbarSetting;
    
    // Runtime state
    private SortButton sortButton;
    private boolean onContainerScreen = false;
    
    public SortInventoryModule() {
        super(
            "sort_inventory",
            "Sort Inventory",
            "Adds a button to sort your inventory.\n" +
            "Works on both inventory and container screens.\n" +
            "Hold ALT + Scroll to change sort mode.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Initialize settings
        this.sortModeSetting = new ModuleSetting.EnumSetting<>(
            "sort_mode",
            "Sort Mode",
            "The method used to sort items",
            SortMode.NAME,
            SortMode.class
        );
        
        this.includeHotbarSetting = new ModuleSetting.BooleanSetting(
            "include_hotbar",
            "Include Hotbar",
            "Whether to include the hotbar in sorting",
            false
        );
        
        registerSetting(sortModeSetting);
        registerSetting(includeHotbarSetting);
    }
    
    /**
     * Creates the sort inventory button for a container screen.
     *
     * @param screen The screen to add the button to
     * @param anchorX The anchor X position (right edge of container)
     * @param anchorY The anchor Y position (top of container)
     * @param isContainerScreen Whether this is a container screen (vs player inventory)
     * @param addDrawableChild Callback to add the button
     */
    public void createButton(HandledScreen<?> screen, int anchorX, int anchorY,
                             boolean isContainerScreen, Consumer<SortButton> addDrawableChild) {
        this.onContainerScreen = isContainerScreen;
        
        // Use different widget ID based on screen type
        String widgetId = isContainerScreen ? SORT_INV_CONTAINER_WIDGET_ID : SORT_INV_WIDGET_ID;
        
        // Get per-widget settings with different defaults based on screen type
        // On container: position near Steal/Store buttons
        // On inventory: position at top-right of inventory area
        int defaultOffsetX = isContainerScreen ? 4 : -24;
        int defaultOffsetY = isContainerScreen ? 44 : 0;
        
        WidgetSettings settings = getWidgetSettings(widgetId, defaultOffsetX, defaultOffsetY);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        sortButton = new SortButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "⇅",  // Sort icon (up-down arrows)
            "Sort Inventory",
            sortModeSetting.getValue(),
            mode -> performSort(screen, mode),
            this::onModeChanged
        );
        
        String displayName = isContainerScreen ? "Sort Inventory (Container)" : "Sort Inventory";
        sortButton.setEditorInfo(this, widgetId, displayName, anchorX, anchorY);
        
        addDrawableChild.accept(sortButton);
    }
    
    /**
     * Gets the button created by this module.
     */
    public List<SortButton> getButtons() {
        List<SortButton> buttons = new ArrayList<>();
        if (sortButton != null) buttons.add(sortButton);
        return buttons;
    }
    
    /**
     * Gets the current sort button.
     */
    public SortButton getSortButton() {
        return sortButton;
    }
    
    /**
     * Performs the sort operation.
     */
    private void performSort(HandledScreen<?> screen, SortMode mode) {
        SortingUtils.sortPlayerInventory(
            screen.getScreenHandler(), 
            mode, 
            includeHotbarSetting.getValue()
        );
    }
    
    /**
     * Called when sort mode changes via Alt+Scroll.
     */
    private void onModeChanged() {
        if (sortButton != null) {
            sortModeSetting.setValue(sortButton.getCurrentMode());
            ModuleManager.getInstance().saveConfig();
        }
    }
    
    /**
     * Gets the current sort mode.
     */
    public SortMode getSortMode() {
        return sortModeSetting.getValue();
    }
    
    /**
     * Sets the sort mode.
     */
    public void setSortMode(SortMode mode) {
        sortModeSetting.setValue(mode);
        if (sortButton != null) {
            sortButton.setMode(mode);
        }
    }
    
    /**
     * @return Whether hotbar is included in sorting
     */
    public boolean isIncludeHotbar() {
        return includeHotbarSetting.getValue();
    }
    
    /**
     * Handles mouse scroll for the sort button.
     */
    public boolean handleScroll(double mouseX, double mouseY, double amount) {
        if (sortButton != null) {
            return sortButton.handleScroll(mouseX, mouseY, amount);
        }
        return false;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        sortButton = null;
    }
}
