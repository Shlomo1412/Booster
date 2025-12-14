package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Module that displays the estimated time until furnace smelting is complete.
 * Works with Furnace, Blast Furnace, and Smoker screens.
 */
public class EstimatedFuelTimeModule extends GUIModule {
    
    public static final String ESTIMATED_TIME_WIDGET_ID = "estimated_fuel_time";
    
    private final ModuleSetting.ColorSetting textColorSetting;
    private final ModuleSetting.BooleanSetting showBackgroundSetting;
    private final ModuleSetting.BooleanSetting showIconSetting;
    
    private BoosterButton timeDisplay;
    
    // Current furnace state for rendering
    private int currentCookTime = 0;
    private int totalCookTime = 0;
    private int remainingItemsToCook = 0;
    
    // Time tracking for smooth countdown
    private long lastCalculationTime = 0;
    private int lastInputCount = -1;
    private int lastFuelCount = -1;
    private float cachedTotalSeconds = 0;
    private boolean wasProcessing = false;
    
    public EstimatedFuelTimeModule() {
        super(
            "estimated_fuel_time",
            "Estimated Fuel Time",
            "Shows how much time is left until the furnace finishes smelting.\n" +
            "Works with Furnace, Blast Furnace, and Smoker.",
            true,
            60,  // Default width
            16   // Default height
        );
        
        // Text color setting
        this.textColorSetting = new ModuleSetting.ColorSetting(
            "text_color",
            "Text Color",
            "Color of the time text",
            0xFFFFFFFF
        );
        registerSetting(textColorSetting);
        
        // Show background setting
        this.showBackgroundSetting = new ModuleSetting.BooleanSetting(
            "show_background",
            "Show Background",
            "Show a background behind the time text",
            true
        );
        registerSetting(showBackgroundSetting);
        
        // Show icon setting
        this.showIconSetting = new ModuleSetting.BooleanSetting(
            "show_icon",
            "Show Icon",
            "Show a clock icon before the time",
            true
        );
        registerSetting(showIconSetting);
    }
    
    /**
     * Checks if the given screen is a furnace-type screen.
     */
    public static boolean isFurnaceScreen(HandledScreen<?> screen) {
        return screen instanceof AbstractFurnaceScreen<?>;
    }
    
    /**
     * Creates the time display widget for the furnace screen.
     */
    public void createWidget(HandledScreen<?> screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        if (!isFurnaceScreen(screen)) {
            return;
        }
        
        WidgetSettings settings = getWidgetSettings(ESTIMATED_TIME_WIDGET_ID, 0, -20);
        
        int widgetX = anchorX + settings.getOffsetX();
        int widgetY = anchorY + settings.getOffsetY();
        
        timeDisplay = new BoosterButton(
            widgetX, widgetY,
            settings.getWidth(), settings.getHeight(),
            "⏱",
            "Est. Time",
            "Shows estimated time until smelting completes",
            button -> {} // No action on click
        );
        
        // Apply display mode
        timeDisplay.setDisplayMode(settings.getDisplayMode());
        
        timeDisplay.setEditorInfo(this, ESTIMATED_TIME_WIDGET_ID, "Est. Time", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(timeDisplay);
        
        addDrawableChild.accept(timeDisplay);
    }
    
    /**
     * Updates the furnace state for time calculation.
     */
    public void updateFurnaceState(AbstractFurnaceScreenHandler handler) {
        // Get progress from the handler
        // Property indices for AbstractFurnaceScreenHandler:
        // 0 = fuel burn time remaining
        // 1 = total fuel burn time
        // 2 = current cook progress
        // 3 = total cook time
        
        this.currentCookTime = (int) handler.getCookProgress();
        this.totalCookTime = 200; // Default cook time in ticks (10 seconds for normal furnace)
        
        // Count items in input slot that need cooking
        if (!handler.getSlot(0).getStack().isEmpty()) {
            this.remainingItemsToCook = handler.getSlot(0).getStack().getCount();
        } else {
            this.remainingItemsToCook = 0;
        }
    }
    
    /**
     * Renders the estimated time text.
     * This should be called from the mixin's render method.
     */
    public void renderTime(DrawContext context, AbstractFurnaceScreenHandler handler, int screenX, int screenY) {
        if (timeDisplay == null || !isEnabled()) {
            return;
        }
        
        updateFurnaceState(handler);
        
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        
        // Calculate remaining time
        String timeText = calculateTimeText(handler);
        
        // Build display text (icon is already shown by the button, so only add it if we're drawing separately)
        StringBuilder displayText = new StringBuilder();
        if (showIconSetting.getValue()) {
            displayText.append("⏱ ");
        }
        displayText.append(timeText);
        
        // Update button message to show the full text
        timeDisplay.setMessage(net.minecraft.text.Text.literal(displayText.toString()));
        
        // Calculate the minimum width needed and update if necessary
        int neededWidth = textRenderer.getWidth(displayText.toString()) + 8; // 4px padding on each side
        if (timeDisplay.getWidth() < neededWidth) {
            timeDisplay.setWidth(neededWidth);
        }
    }
    
    /**
     * Calculates the time text based on furnace state.
     * Uses time-based interpolation for smooth countdown even when game is paused.
     */
    private String calculateTimeText(AbstractFurnaceScreenHandler handler) {
        // Check if there are items to smelt
        if (handler.getSlot(0).getStack().isEmpty()) {
            resetTimeTracking();
            return "No items";
        }
        
        // Check if burning (has fuel)
        boolean isBurning = handler.isBurning();
        if (!isBurning) {
            resetTimeTracking();
            return "No fuel";
        }
        
        // Get current slot counts
        int currentInputCount = handler.getSlot(0).getStack().getCount();
        int currentFuelCount = handler.getSlot(1).getStack().getCount();
        
        long currentTime = System.currentTimeMillis();
        
        // Check if we need to recalculate the base time
        // Recalculate when: first time, items changed, or wasn't processing before
        boolean needsRecalculation = !wasProcessing ||
            currentInputCount != lastInputCount ||
            currentFuelCount != lastFuelCount ||
            lastCalculationTime == 0;
        
        if (needsRecalculation) {
            // Calculate the actual remaining time from furnace state
            float cookProgressScaled = handler.getCookProgress();
            int ticksPerItem = getTotalCookTime(handler);
            
            int maxProgress = 24;
            int currentProgress = (int) cookProgressScaled;
            int remainingProgressForCurrent = maxProgress - currentProgress;
            
            float progressRatio = (float) remainingProgressForCurrent / maxProgress;
            int ticksForCurrentItem = (int) (ticksPerItem * progressRatio);
            
            int ticksForRemainingItems = (currentInputCount > 1 ? (currentInputCount - 1) : 0) * ticksPerItem;
            
            int totalTicks = ticksForCurrentItem + ticksForRemainingItems;
            cachedTotalSeconds = totalTicks / 20.0f;
            
            lastCalculationTime = currentTime;
            lastInputCount = currentInputCount;
            lastFuelCount = currentFuelCount;
            wasProcessing = true;
        }
        
        // Calculate elapsed time since last calculation and subtract from cached time
        float elapsedSeconds = (currentTime - lastCalculationTime) / 1000.0f;
        float remainingSeconds = cachedTotalSeconds - elapsedSeconds;
        
        // Clamp to 0
        if (remainingSeconds < 0) {
            remainingSeconds = 0;
        }
        
        // Format the time
        if (remainingSeconds < 1) {
            return String.format("%.1fs", remainingSeconds);
        } else if (remainingSeconds < 60) {
            return String.format("%.1fs", remainingSeconds);
        } else {
            int mins = (int) (remainingSeconds / 60);
            float secs = remainingSeconds % 60;
            return String.format("%dm %.0fs", mins, secs);
        }
    }
    
    /**
     * Resets the time tracking state.
     */
    private void resetTimeTracking() {
        lastCalculationTime = 0;
        lastInputCount = -1;
        lastFuelCount = -1;
        cachedTotalSeconds = 0;
        wasProcessing = false;
    }
    
    /**
     * Gets the total cook time based on furnace type.
     */
    private int getTotalCookTime(AbstractFurnaceScreenHandler handler) {
        // Normal furnace: 200 ticks (10 seconds)
        // Blast furnace and smoker: 100 ticks (5 seconds)
        // We can check the class name to determine type
        String className = handler.getClass().getSimpleName();
        if (className.contains("Blast") || className.contains("Smoker")) {
            return 100; // 5 seconds
        }
        return 200; // 10 seconds
    }
    
    /**
     * Gets the current time text for the pinned overlay.
     */
    public String getCurrentTimeText(AbstractFurnaceScreenHandler handler) {
        return calculateTimeText(handler);
    }
    
    /**
     * Checks if furnace is currently processing.
     */
    public boolean isProcessing(AbstractFurnaceScreenHandler handler) {
        return handler.isBurning() || handler.getCookProgress() > 0;
    }
    
    @Override
    public Set<String> getWidgetIds() {
        Set<String> ids = new HashSet<>();
        ids.add(ESTIMATED_TIME_WIDGET_ID);
        return ids;
    }
}
