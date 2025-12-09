package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterProgressBar;

/**
 * Module that shows a progress bar indicating how full the container is.
 * Supports customizable fill direction, fill color, and background color.
 */
public class InventoryProgressModule extends GUIModule {
    
    public static final String PROGRESS_WIDGET_ID = "progressbar";
    
    /**
     * Direction in which the progress bar fills.
     */
    public enum FillDirection {
        UP("Up"),
        DOWN("Down"),
        LEFT("Left"),
        RIGHT("Right");
        
        private final String displayName;
        
        FillDirection(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // Module settings
    private final ModuleSetting.EnumSetting<FillDirection> fillDirection;
    private final ModuleSetting.ColorSetting fillColor;
    private final ModuleSetting.ColorSetting backgroundColor;
    
    // Runtime state
    private HandledScreen<?> currentScreen;
    private BoosterProgressBar progressBarWidget;
    private int currentX, currentY, currentWidth, currentHeight;
    
    public InventoryProgressModule() {
        super(
            "inventory_progress",
            "Inventory Progress",
            "Shows a progress bar indicating how full the container is.\n" +
            "Customize the fill direction and colors in the editor.",
            true,
            8,    // Default width (for vertical bar)
            80    // Default height
        );
        
        // Initialize settings
        this.fillDirection = new ModuleSetting.EnumSetting<>(
            "fill_direction",
            "Fill Direction",
            "Direction the progress bar fills",
            FillDirection.UP,
            FillDirection.class
        );
        
        this.fillColor = new ModuleSetting.ColorSetting(
            "fill_color",
            "Fill Color",
            "Color of the filled portion",
            0xFF44AA44  // Green
        );
        
        this.backgroundColor = new ModuleSetting.ColorSetting(
            "background_color",
            "Background Color",
            "Color of the empty portion",
            0xFF333333  // Dark gray
        );
        
        // Register settings
        registerSetting(fillDirection);
        registerSetting(fillColor);
        registerSetting(backgroundColor);
    }
    
    /**
     * Creates the progress bar widget for a container screen.
     * Returns the widget so it can be added to the screen.
     * 
     * The positioning is responsive yet respects user preferences:
     * - User's saved width/height are ALWAYS used
     * - User's saved offsets are applied relative to container
     * - If the resulting position would be off-screen, it's clamped to stay visible
     */
    public BoosterProgressBar createProgressBar(HandledScreen<?> screen, int anchorX, int anchorY) {
        this.currentScreen = screen;
        
        // Get screen dimensions
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        
        // Get widget settings - default position to the left of the container
        WidgetSettings settings = getWidgetSettings(PROGRESS_WIDGET_ID, -12, 0);
        
        // Always use user's saved size
        this.currentWidth = settings.getWidth();
        this.currentHeight = settings.getHeight();
        
        // Calculate position based on user's saved offsets
        this.currentX = anchorX + settings.getOffsetX();
        this.currentY = anchorY + settings.getOffsetY();
        
        // Responsive clamping: keep widget fully on-screen
        if (currentX < 2) {
            currentX = 2;
        } else if (currentX + currentWidth > screenWidth - 2) {
            currentX = screenWidth - currentWidth - 2;
        }
        
        if (currentY < 2) {
            currentY = 2;
        } else if (currentY + currentHeight > screenHeight - 2) {
            currentY = screenHeight - currentHeight - 2;
        }
        
        // Create the draggable widget
        progressBarWidget = new BoosterProgressBar(
            this, screen,
            currentX, currentY, currentWidth, currentHeight,
            anchorX, anchorY
        );
        
        return progressBarWidget;
    }
    
    /**
     * Gets the progress bar widget.
     */
    public BoosterProgressBar getProgressBarWidget() {
        return progressBarWidget;
    }
    
    /**
     * Calculates the fill percentage of the container (0.0 to 1.0).
     */
    public float calculateFillPercentage() {
        if (currentScreen == null) {
            return 0f;
        }
        
        var handler = currentScreen.getScreenHandler();
        int totalSlots = 0;
        int filledSlots = 0;
        
        for (Slot slot : handler.slots) {
            // Only count container slots, not player inventory
            if (!(slot.inventory instanceof PlayerInventory)) {
                totalSlots++;
                if (slot.hasStack()) {
                    filledSlots++;
                }
            }
        }
        
        if (totalSlots == 0) {
            return 0f;
        }
        
        return (float) filledSlots / totalSlots;
    }
    
    /**
     * Renders the progress bar.
     */
    public void render(DrawContext context, int anchorX, int anchorY) {
        if (currentScreen == null) {
            return;
        }
        
        // Recalculate position based on current anchor
        WidgetSettings settings = getWidgetSettings(PROGRESS_WIDGET_ID);
        if (settings == null) {
            settings = getWidgetSettings(PROGRESS_WIDGET_ID, -12, 0);
        }
        
        int x = anchorX + settings.getOffsetX();
        int y = anchorY + settings.getOffsetY();
        int width = settings.getWidth();
        int height = settings.getHeight();
        
        float fillPercent = calculateFillPercentage();
        int bgColor = backgroundColor.getValue();
        int fgColor = fillColor.getValue();
        FillDirection direction = fillDirection.getValue();
        
        // Draw background
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Draw fill based on direction
        int fillX1 = x, fillY1 = y, fillX2 = x + width, fillY2 = y + height;
        
        switch (direction) {
            case UP -> {
                int fillHeight = (int) (height * fillPercent);
                fillY1 = y + height - fillHeight;
                fillY2 = y + height;
            }
            case DOWN -> {
                int fillHeight = (int) (height * fillPercent);
                fillY1 = y;
                fillY2 = y + fillHeight;
            }
            case LEFT -> {
                int fillWidth = (int) (width * fillPercent);
                fillX1 = x + width - fillWidth;
                fillX2 = x + width;
            }
            case RIGHT -> {
                int fillWidth = (int) (width * fillPercent);
                fillX1 = x;
                fillX2 = x + fillWidth;
            }
        }
        
        // Draw fill
        if (fillPercent > 0) {
            context.fill(fillX1, fillY1, fillX2, fillY2, fgColor);
        }
        
        // Draw border
        int borderColor = 0xFF000000;
        context.fill(x, y, x + width, y + 1, borderColor);           // Top
        context.fill(x, y + height - 1, x + width, y + height, borderColor); // Bottom
        context.fill(x, y, x + 1, y + height, borderColor);          // Left
        context.fill(x + width - 1, y, x + width, y + height, borderColor);  // Right
        
        // Store current position for editor
        this.currentX = x;
        this.currentY = y;
        this.currentWidth = width;
        this.currentHeight = height;
    }
    
    /**
     * Gets info about the current container fullness.
     */
    public String getFullnessInfo() {
        if (currentScreen == null) {
            return "N/A";
        }
        
        var handler = currentScreen.getScreenHandler();
        int totalSlots = 0;
        int filledSlots = 0;
        
        for (Slot slot : handler.slots) {
            if (!(slot.inventory instanceof PlayerInventory)) {
                totalSlots++;
                if (slot.hasStack()) {
                    filledSlots++;
                }
            }
        }
        
        return filledSlots + "/" + totalSlots + " (" + (int)(calculateFillPercentage() * 100) + "%)";
    }
    
    // Getters for settings
    public ModuleSetting.EnumSetting<FillDirection> getFillDirectionSetting() {
        return fillDirection;
    }
    
    public ModuleSetting.ColorSetting getFillColorSetting() {
        return fillColor;
    }
    
    public ModuleSetting.ColorSetting getBackgroundColorSetting() {
        return backgroundColor;
    }
    
    // Position getters for editor mode
    public int getCurrentX() { return currentX; }
    public int getCurrentY() { return currentY; }
    public int getCurrentWidth() { return currentWidth; }
    public int getCurrentHeight() { return currentHeight; }
    
    @Override
    protected void onEnable() {
        // Nothing special needed
    }
    
    @Override
    protected void onDisable() {
        currentScreen = null;
        progressBarWidget = null;
    }
}
