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
     */
    private String calculateTimeText(AbstractFurnaceScreenHandler handler) {
        // Check if there are items to smelt
        if (handler.getSlot(0).getStack().isEmpty()) {
            return "No items";
        }
        
        // Check if burning (has fuel)
        boolean isBurning = handler.isBurning();
        if (!isBurning) {
            return "No fuel";
        }
        
        // Get actual cook progress from handler's property delegate
        // getCookProgress() returns scaled value for rendering (0-24)
        // We need to calculate based on actual progress
        float cookProgressScaled = handler.getCookProgress(); // 0.0 to 1.0 approximately, scaled to arrow width
        
        // The cook progress is scaled to 24 (arrow width), so we need to reverse it
        // cookProgress of 24 means done, 0 means just started
        int ticksPerItem = getTotalCookTime(handler);
        
        // Calculate remaining ticks for current item
        // getCookProgress returns a float that represents progress (scaled for rendering)
        // We calculate remaining based on how much progress is left
        int maxProgress = 24; // Arrow width used for scaling
        int currentProgress = (int) cookProgressScaled;
        int remainingProgressForCurrent = maxProgress - currentProgress;
        
        float progressRatio = (float) remainingProgressForCurrent / maxProgress;
        int ticksForCurrentItem = (int) (ticksPerItem * progressRatio);
        
        // Add time for remaining items in input slot (minus the one currently cooking)
        int inputCount = handler.getSlot(0).getStack().getCount();
        int ticksForRemainingItems = (inputCount > 1 ? (inputCount - 1) : 0) * ticksPerItem;
        
        int totalTicks = ticksForCurrentItem + ticksForRemainingItems;
        
        // Convert to seconds
        float seconds = totalTicks / 20.0f;
        
        if (seconds < 1) {
            return String.format("%.1fs", seconds);
        } else if (seconds < 60) {
            return String.format("%.1fs", seconds);
        } else {
            int mins = (int) (seconds / 60);
            float secs = seconds % 60;
            return String.format("%dm %.0fs", mins, secs);
        }
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
