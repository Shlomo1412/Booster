package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Module that shows the player's inventory at the time of death.
 * Displays equipped armor, hotbar, and main inventory.
 */
public class DeathInventoryModule extends GUIModule {
    
    public static final String DEATH_INV_WIDGET_ID = "death_inventory";
    
    // Static storage for death inventory
    private static List<ItemStack> lastDeathInventory = null;
    private static ItemStack[] lastDeathArmor = null;
    private static ItemStack lastDeathOffhand = null;
    
    private final ModuleSetting.BooleanSetting showArmorSetting;
    private final ModuleSetting.BooleanSetting showHotbarSetting;
    private final ModuleSetting.BooleanSetting showFullInventorySetting;
    private final ModuleSetting.NumberSetting scaleSetting;
    
    private BoosterButton inventoryButton;
    private boolean showingInventory = false;
    
    public DeathInventoryModule() {
        super(
            "death_inventory",
            "Death Inventory",
            "Shows your inventory contents when you died.\n" +
            "Click to toggle the inventory display.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Show armor setting
        this.showArmorSetting = new ModuleSetting.BooleanSetting(
            "show_armor",
            "Show Armor",
            "Whether to show equipped armor",
            true
        );
        registerSetting(showArmorSetting);
        
        // Show hotbar setting
        this.showHotbarSetting = new ModuleSetting.BooleanSetting(
            "show_hotbar",
            "Show Hotbar",
            "Whether to show the hotbar items",
            true
        );
        registerSetting(showHotbarSetting);
        
        // Show full inventory setting
        this.showFullInventorySetting = new ModuleSetting.BooleanSetting(
            "show_full_inventory",
            "Show Full Inventory",
            "Whether to show the full inventory (not just hotbar)",
            true
        );
        registerSetting(showFullInventorySetting);
        
        // Scale setting
        this.scaleSetting = new ModuleSetting.NumberSetting(
            "scale",
            "Item Scale",
            "Size of inventory items (percent)",
            100,
            50,
            150
        );
        registerSetting(scaleSetting);
    }
    
    /**
     * Records the player's inventory at death.
     */
    public static void recordDeathInventory(List<ItemStack> inventory, ItemStack[] armor, ItemStack offhand) {
        lastDeathInventory = new ArrayList<>();
        for (ItemStack stack : inventory) {
            lastDeathInventory.add(stack.copy());
        }
        
        lastDeathArmor = new ItemStack[armor.length];
        for (int i = 0; i < armor.length; i++) {
            lastDeathArmor[i] = armor[i].copy();
        }
        
        lastDeathOffhand = offhand.copy();
    }
    
    /**
     * Checks if there's a recorded death inventory.
     */
    public static boolean hasDeathInventory() {
        return lastDeathInventory != null && !lastDeathInventory.isEmpty();
    }
    
    /**
     * Creates the inventory button for the death screen.
     */
    public void createButton(DeathScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(DEATH_INV_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        inventoryButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🎒",
            "Inventory",
            "Show your inventory at the time of death.\n" +
            "Click to toggle the display.",
            button -> toggleInventory()
        );
        
        // Apply display mode
        inventoryButton.setDisplayMode(settings.getDisplayMode());
        
        inventoryButton.setEditorInfo(this, DEATH_INV_WIDGET_ID, "Death Inventory", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(inventoryButton);
        
        addDrawableChild.accept(inventoryButton);
    }
    
    /**
     * Toggles the inventory display.
     */
    private void toggleInventory() {
        showingInventory = !showingInventory;
    }
    
    /**
     * Renders the inventory panel if showing.
     */
    public void renderInventoryPanel(DrawContext context, int mouseX, int mouseY) {
        if (!showingInventory || inventoryButton == null || !hasDeathInventory()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        float scale = scaleSetting.getValue() / 100.0f;
        int slotSize = (int) (18 * scale);
        int padding = 8;
        
        // Calculate panel dimensions
        int cols = 9;
        int rows = showFullInventorySetting.getValue() ? 4 : 1; // 4 rows for full, 1 for hotbar only
        int armorRows = showArmorSetting.getValue() ? 1 : 0;
        
        int contentWidth = cols * slotSize;
        int contentHeight = (rows + armorRows) * slotSize + (armorRows > 0 ? 4 : 0);
        
        int panelWidth = contentWidth + padding * 2;
        int panelHeight = contentHeight + padding * 2 + 16; // +16 for title
        
        // Position panel to the left of button
        int x = inventoryButton.getX() - panelWidth - 8;
        int y = inventoryButton.getY();
        
        // Keep on screen
        if (x < 5) x = inventoryButton.getX() + inventoryButton.getWidth() + 8;
        if (y + panelHeight > client.getWindow().getScaledHeight() - 5) {
            y = client.getWindow().getScaledHeight() - panelHeight - 5;
        }
        
        // Background
        context.fill(x, y, x + panelWidth, y + panelHeight, 0xF0181818);
        context.fill(x + 1, y + 1, x + panelWidth - 1, y + 2, 0xFF3A3A3A);
        context.fill(x + 1, y + 1, x + 2, y + panelHeight - 1, 0xFF3A3A3A);
        context.drawBorder(x, y, panelWidth, panelHeight, 0xFF505050);
        
        // Title bar
        context.fill(x + 1, y + 1, x + panelWidth - 1, y + 14, 0xFF252525);
        context.fill(x + 1, y + 14, x + panelWidth - 1, y + 15, 0xFF404040);
        context.drawCenteredTextWithShadow(client.textRenderer, "§e§lDeath Inventory", 
            x + panelWidth / 2, y + 3, 0xFFFFFF);
        
        int contentY = y + 18;
        int contentX = x + padding;
        
        // Render armor if enabled
        if (showArmorSetting.getValue() && lastDeathArmor != null) {
            // Render in order: Helmet, Chestplate, Leggings, Boots, Offhand
            for (int i = 3; i >= 0; i--) {
                int slotX = contentX + (3 - i) * slotSize;
                renderSlot(context, slotX, contentY, slotSize, lastDeathArmor[i], mouseX, mouseY);
            }
            // Offhand
            if (lastDeathOffhand != null) {
                renderSlot(context, contentX + 5 * slotSize, contentY, slotSize, lastDeathOffhand, mouseX, mouseY);
            }
            contentY += slotSize + 4;
        }
        
        // Render hotbar (slots 0-8)
        if (showHotbarSetting.getValue() && lastDeathInventory != null) {
            for (int i = 0; i < 9 && i < lastDeathInventory.size(); i++) {
                int slotX = contentX + i * slotSize;
                renderSlot(context, slotX, contentY, slotSize, lastDeathInventory.get(i), mouseX, mouseY);
            }
            contentY += slotSize;
        }
        
        // Render main inventory (slots 9-35)
        if (showFullInventorySetting.getValue() && lastDeathInventory != null) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    int index = 9 + row * 9 + col;
                    if (index < lastDeathInventory.size()) {
                        int slotX = contentX + col * slotSize;
                        int slotY = contentY + row * slotSize;
                        renderSlot(context, slotX, slotY, slotSize, lastDeathInventory.get(index), mouseX, mouseY);
                    }
                }
            }
        }
        
        // Close hint
        context.drawCenteredTextWithShadow(client.textRenderer, "§8Click button to close", 
            x + panelWidth / 2, y + panelHeight - 11, 0x666666);
    }
    
    /**
     * Renders a single inventory slot.
     */
    private void renderSlot(DrawContext context, int x, int y, int size, ItemStack stack, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Slot background
        context.fill(x, y, x + size - 1, y + size - 1, 0xFF373737);
        context.fill(x, y, x + size - 2, y + 1, 0xFF5A5A5A);
        context.fill(x, y, x + 1, y + size - 2, 0xFF5A5A5A);
        context.fill(x + size - 2, y + 1, x + size - 1, y + size - 1, 0xFF2A2A2A);
        context.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, 0xFF2A2A2A);
        
        if (stack != null && !stack.isEmpty()) {
            // Calculate item position (centered in slot)
            int itemX = x + (size - 16) / 2;
            int itemY = y + (size - 16) / 2;
            
            // Render item
            context.drawItem(stack, itemX, itemY);
            context.drawStackOverlay(client.textRenderer, stack, itemX, itemY);
            
            // Tooltip on hover
            if (mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size) {
                context.fill(x, y, x + size - 1, y + size - 1, 0x40FFFFFF);
            }
        }
    }
    
    /**
     * Handles click to check if we should show tooltip.
     */
    public boolean handleClick(double mouseX, double mouseY) {
        // Just close when clicking the button (handled by button itself)
        return false;
    }
    
    /**
     * Returns whether the inventory panel is showing.
     */
    public boolean isShowingInventory() {
        return showingInventory;
    }
    
    /**
     * Closes the inventory panel.
     */
    public void closeInventory() {
        showingInventory = false;
    }
    
    /**
     * Gets the button.
     */
    public BoosterButton getButton() {
        return inventoryButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        inventoryButton = null;
        showingInventory = false;
    }
    
    /**
     * Gets the last recorded death inventory.
     */
    public static List<ItemStack> getLastDeathInventory() {
        return lastDeathInventory;
    }
    
    /**
     * Gets the last recorded death armor.
     */
    public static ItemStack[] getLastDeathArmor() {
        return lastDeathArmor;
    }
    
    /**
     * Gets the last recorded death offhand item.
     */
    public static ItemStack getLastDeathOffhand() {
        return lastDeathOffhand;
    }
}
