package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.shlomo1412.booster.client.module.Module;
import net.shlomo1412.booster.client.module.ModuleSetting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Module that shows the player's inventory when holding a keybind.
 * Displays the main inventory (not hotbar) above the hotbar, similar to
 * the death screen inventory view.
 */
public class ShowInventoryModule extends Module {
    
    private final ModuleSetting.NumberSetting keybindSetting;
    private final ModuleSetting.BooleanSetting showBackgroundSetting;
    private final ModuleSetting.NumberSetting backgroundOpacitySetting;
    private final ModuleSetting.NumberSetting scaleSetting;
    private final ModuleSetting.NumberSetting verticalOffsetSetting;
    
    private final List<ModuleSetting<?>> settings = new ArrayList<>();
    
    // Slot size constants
    private static final int SLOT_SIZE = 18;
    private static final int INVENTORY_COLS = 9;
    private static final int INVENTORY_ROWS = 3; // Main inventory has 3 rows (not counting hotbar)
    
    public ShowInventoryModule() {
        super(
            "show_inventory",
            "Show Inventory",
            "Shows your inventory above the hotbar when holding a key.\n" +
            "Displays main inventory slots (not hotbar).",
            true
        );
        
        // Keybind setting (stores GLFW key code)
        this.keybindSetting = new ModuleSetting.NumberSetting(
            "keybind",
            "Keybind",
            "Key to hold to show inventory (GLFW key code). Default: Left Alt (342)",
            GLFW.GLFW_KEY_LEFT_ALT,
            0,
            500
        );
        settings.add(keybindSetting);
        
        // Show background setting
        this.showBackgroundSetting = new ModuleSetting.BooleanSetting(
            "show_background",
            "Show Background",
            "Show a dark background behind the inventory",
            true
        );
        settings.add(showBackgroundSetting);
        
        // Background opacity setting
        this.backgroundOpacitySetting = new ModuleSetting.NumberSetting(
            "background_opacity",
            "Background Opacity",
            "Opacity of the background (0-100%)",
            70,
            0,
            100
        );
        settings.add(backgroundOpacitySetting);
        
        // Scale setting
        this.scaleSetting = new ModuleSetting.NumberSetting(
            "scale",
            "Scale",
            "Scale of the inventory display (50-150%)",
            100,
            50,
            150
        );
        settings.add(scaleSetting);
        
        // Vertical offset setting
        this.verticalOffsetSetting = new ModuleSetting.NumberSetting(
            "vertical_offset",
            "Vertical Offset",
            "Distance above the hotbar",
            4,
            0,
            50
        );
        settings.add(verticalOffsetSetting);
    }
    
    /**
     * Returns whether the keybind is currently held.
     */
    public boolean isKeyHeld() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getWindow() == null) return false;
        
        long windowHandle = client.getWindow().getHandle();
        int keyCode = keybindSetting.getValue().intValue();
        
        return InputUtil.isKeyPressed(windowHandle, keyCode);
    }
    
    /**
     * Renders the inventory overlay above the hotbar.
     * Should be called from the HUD render mixin.
     */
    public void renderInventoryOverlay(DrawContext context) {
        if (!isEnabled() || !isKeyHeld()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null || client.currentScreen != null) {
            return; // Don't show when in a screen
        }
        
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        // Calculate scale
        float scale = scaleSetting.getValue().floatValue() / 100.0f;
        int scaledSlotSize = (int) (SLOT_SIZE * scale);
        
        // Calculate inventory dimensions
        int inventoryWidth = INVENTORY_COLS * scaledSlotSize;
        int inventoryHeight = INVENTORY_ROWS * scaledSlotSize;
        
        // Calculate hotbar position (vanilla hotbar is centered, 182 pixels wide)
        int hotbarWidth = 182;
        int hotbarY = screenHeight - 22; // Hotbar is 22 pixels from bottom
        
        // Position inventory above hotbar, centered
        int invX = (screenWidth - inventoryWidth) / 2;
        int invY = hotbarY - inventoryHeight - verticalOffsetSetting.getValue().intValue();
        
        // Draw background if enabled
        if (showBackgroundSetting.getValue()) {
            int opacity = backgroundOpacitySetting.getValue().intValue();
            int alpha = (int) (255 * (opacity / 100.0));
            int backgroundColor = (alpha << 24) | 0x000000;
            
            // Draw background with slight padding
            context.fill(invX - 4, invY - 4, invX + inventoryWidth + 4, invY + inventoryHeight + 4, backgroundColor);
            
            // Draw border
            int borderColor = 0xFF555555;
            context.fill(invX - 5, invY - 5, invX + inventoryWidth + 5, invY - 4, borderColor); // Top
            context.fill(invX - 5, invY + inventoryHeight + 4, invX + inventoryWidth + 5, invY + inventoryHeight + 5, borderColor); // Bottom
            context.fill(invX - 5, invY - 4, invX - 4, invY + inventoryHeight + 4, borderColor); // Left
            context.fill(invX + inventoryWidth + 4, invY - 4, invX + inventoryWidth + 5, invY + inventoryHeight + 4, borderColor); // Right
        }
        
        // Draw inventory slots
        // Main inventory is slots 9-35 (after hotbar 0-8)
        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < INVENTORY_COLS; col++) {
                int slotIndex = 9 + row * INVENTORY_COLS + col; // Inventory slots start at 9
                
                int slotX = invX + col * scaledSlotSize;
                int slotY = invY + row * scaledSlotSize;
                
                // Draw slot background
                context.fill(slotX, slotY, slotX + scaledSlotSize - 1, slotY + scaledSlotSize - 1, 0xFF8B8B8B);
                context.fill(slotX + 1, slotY + 1, slotX + scaledSlotSize - 1, slotY + scaledSlotSize - 1, 0xFF373737);
                context.fill(slotX + 1, slotY + 1, slotX + scaledSlotSize - 2, slotY + scaledSlotSize - 2, 0xFF8B8B8B);
                
                // Get item from inventory
                ItemStack stack = player.getInventory().getStack(slotIndex);
                
                if (!stack.isEmpty()) {
                    // Calculate centered position for item (items are 16x16)
                    int itemX = slotX + (scaledSlotSize - 16) / 2;
                    int itemY = slotY + (scaledSlotSize - 16) / 2;
                    
                    // Draw item
                    context.drawItem(stack, itemX, itemY);
                    context.drawStackOverlay(textRenderer, stack, itemX, itemY);
                }
            }
        }
    }
    
    /**
     * Gets the display name for the current keybind.
     */
    public String getKeybindName() {
        int keyCode = keybindSetting.getValue().intValue();
        return InputUtil.fromKeyCode(keyCode, 0).getLocalizedText().getString();
    }
    
    /**
     * Returns the list of settings for this module.
     */
    public List<ModuleSetting<?>> getSettings() {
        return settings;
    }
}
