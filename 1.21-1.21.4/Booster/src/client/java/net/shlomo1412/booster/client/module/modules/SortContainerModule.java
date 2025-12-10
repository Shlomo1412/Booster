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
 * Module that adds a Sort Container button to container screens.
 * - Sorts the container's inventory (chest, barrel, etc.)
 * - Alt+Scroll to change sort mode
 * - Only appears on container screens (not player inventory)
 */
public class SortContainerModule extends GUIModule {
    
    // Widget ID for per-widget settings
    public static final String SORT_CONTAINER_WIDGET_ID = "sort_container";
    
    // Settings
    private final ModuleSetting.EnumSetting<SortMode> sortModeSetting;
    
    // Runtime state
    private SortButton sortButton;
    
    public SortContainerModule() {
        super(
            "sort_container",
            "Sort Container",
            "Adds a button to sort the container's contents.\n" +
            "Works on chests, barrels, shulker boxes, etc.\n" +
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
        
        registerSetting(sortModeSetting);
    }
    
    /**
     * Creates the sort container button.
     *
     * @param screen The screen to add the button to
     * @param anchorX The anchor X position (right edge of container)
     * @param anchorY The anchor Y position (top of container)
     * @param addDrawableChild Callback to add the button
     */
    public void createButton(HandledScreen<?> screen, int anchorX, int anchorY,
                             Consumer<SortButton> addDrawableChild) {
        // Get per-widget settings
        // Default: Position below steal/store buttons, above sort-inventory
        WidgetSettings settings = getWidgetSettings(SORT_CONTAINER_WIDGET_ID, 4, 44);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        sortButton = new SortButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "⬓",  // Container sort icon (different from inventory sort)
            "Sort Container",
            sortModeSetting.getValue(),
            mode -> performSort(screen, mode),
            this::onModeChanged
        );
        
        sortButton.setEditorInfo(this, SORT_CONTAINER_WIDGET_ID, "Sort Container", anchorX, anchorY);
        
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
        SortingUtils.sortContainer(screen.getScreenHandler(), mode);
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
