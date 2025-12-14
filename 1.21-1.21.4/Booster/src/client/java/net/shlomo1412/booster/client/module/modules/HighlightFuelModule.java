package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;

import java.util.HashSet;
import java.util.Set;

/**
 * Module that highlights fuel items in the inventory when viewing a furnace screen.
 * Makes it easy to identify which items can be used as fuel.
 * Works with Furnace, Blast Furnace, and Smoker screens.
 */
public class HighlightFuelModule extends GUIModule {
    
    private final ModuleSetting.ColorSetting highlightColorSetting;
    private final ModuleSetting.BooleanSetting showBorderSetting;
    private final ModuleSetting.BooleanSetting showOverlaySetting;
    private final ModuleSetting.NumberSetting overlayOpacitySetting;
    
    // Set of slot indices that contain fuel items (for rendering)
    private final Set<Integer> fuelSlots = new HashSet<>();
    
    public HighlightFuelModule() {
        super(
            "highlight_fuel",
            "Highlight Fuel",
            "Highlights items in your inventory that can be used as fuel.\n" +
            "Works with Furnace, Blast Furnace, and Smoker.",
            true,
            0,   // No button needed
            0
        );
        
        // Highlight color setting
        this.highlightColorSetting = new ModuleSetting.ColorSetting(
            "highlight_color",
            "Highlight Color",
            "Color used to highlight fuel items",
            0xFFFFAA00  // Orange/yellow
        );
        registerSetting(highlightColorSetting);
        
        // Show border setting
        this.showBorderSetting = new ModuleSetting.BooleanSetting(
            "show_border",
            "Show Border",
            "Show a colored border around fuel items",
            true
        );
        registerSetting(showBorderSetting);
        
        // Show overlay setting
        this.showOverlaySetting = new ModuleSetting.BooleanSetting(
            "show_overlay",
            "Show Overlay",
            "Show a semi-transparent overlay on fuel items",
            true
        );
        registerSetting(showOverlaySetting);
        
        // Overlay opacity setting
        this.overlayOpacitySetting = new ModuleSetting.NumberSetting(
            "overlay_opacity",
            "Overlay Opacity",
            "Opacity of the overlay (0-100%)",
            30,
            10,
            80
        );
        registerSetting(overlayOpacitySetting);
    }
    
    /**
     * Updates which slots contain fuel items.
     * Should be called when the screen opens and when inventory changes.
     */
    public void updateFuelSlots(AbstractFurnaceScreenHandler handler) {
        fuelSlots.clear();
        
        // Player inventory starts at slot 3 in furnace screen
        // Slots 0-2 are furnace slots (input, fuel, output)
        for (int i = 3; i < handler.slots.size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (!stack.isEmpty() && SmartFuelModule.isFuel(stack.getItem())) {
                fuelSlots.add(i);
            }
        }
    }
    
    /**
     * Checks if a slot index contains fuel.
     */
    public boolean isSlotFuel(int slotIndex) {
        return fuelSlots.contains(slotIndex);
    }
    
    /**
     * Returns the highlight color with proper alpha.
     */
    public int getHighlightColor() {
        return highlightColorSetting.getValue();
    }
    
    /**
     * Returns the overlay color with adjusted opacity.
     */
    public int getOverlayColor() {
        int color = highlightColorSetting.getValue();
        int opacity = overlayOpacitySetting.getValue().intValue();
        int alpha = (int) (255 * (opacity / 100.0));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
    
    /**
     * Returns whether to show border.
     */
    public boolean shouldShowBorder() {
        return showBorderSetting.getValue();
    }
    
    /**
     * Returns whether to show overlay.
     */
    public boolean shouldShowOverlay() {
        return showOverlaySetting.getValue();
    }
    
    /**
     * Gets all slot indices containing fuel.
     */
    public Set<Integer> getFuelSlots() {
        return new HashSet<>(fuelSlots);
    }
    
    /**
     * Clears the fuel slots when screen closes.
     */
    public void clearFuelSlots() {
        fuelSlots.clear();
    }
    
    /**
     * Renders highlights on all fuel slots.
     * Should be called from the mixin's render method.
     */
    public void renderHighlights(DrawContext context, AbstractFurnaceScreenHandler handler, int screenX, int screenY) {
        if (!isEnabled() || fuelSlots.isEmpty()) {
            return;
        }
        
        for (int slotIndex : fuelSlots) {
            Slot slot = handler.getSlot(slotIndex);
            int slotX = screenX + slot.x;
            int slotY = screenY + slot.y;
            
            // Render overlay if enabled
            if (shouldShowOverlay()) {
                context.fill(slotX, slotY, slotX + 16, slotY + 16, getOverlayColor());
            }
            
            // Render border if enabled
            if (shouldShowBorder()) {
                int borderColor = getHighlightColor();
                // Top border
                context.fill(slotX - 1, slotY - 1, slotX + 17, slotY, borderColor);
                // Bottom border
                context.fill(slotX - 1, slotY + 16, slotX + 17, slotY + 17, borderColor);
                // Left border
                context.fill(slotX - 1, slotY, slotX, slotY + 16, borderColor);
                // Right border
                context.fill(slotX + 16, slotY, slotX + 17, slotY + 16, borderColor);
            }
        }
    }
    
    @Override
    public Set<String> getWidgetIds() {
        // This module doesn't have visible widgets - it's a rendering enhancement
        return new HashSet<>();
    }
}
