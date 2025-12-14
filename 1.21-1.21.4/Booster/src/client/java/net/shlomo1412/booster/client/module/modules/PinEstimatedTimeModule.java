package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Module that pins the estimated smelting time as an overlay on screen.
 * Shows even after exiting the furnace screen.
 * Displays "FINISHED!" in green when complete, then disappears.
 */
public class PinEstimatedTimeModule extends GUIModule {
    
    public static final String PIN_TIME_WIDGET_ID = "pin_estimated_time";
    
    private final ModuleSetting.ColorSetting textColorSetting;
    private final ModuleSetting.ColorSetting finishedColorSetting;
    private final ModuleSetting.NumberSetting finishedDisplayTimeSetting;
    private final ModuleSetting.BooleanSetting showBackgroundSetting;
    private final ModuleSetting.NumberSetting overlayXSetting;
    private final ModuleSetting.NumberSetting overlayYSetting;
    
    private BoosterButton pinButton;
    
    // Tracked furnaces with their estimated completion time
    private static final Map<BlockPos, TrackedFurnace> trackedFurnaces = new HashMap<>();
    
    public PinEstimatedTimeModule() {
        super(
            "pin_estimated_time",
            "Pin Estimated Time",
            "Pins the estimated smelting time as an overlay.\n" +
            "Shows even after exiting the furnace screen.\n" +
            "Displays FINISHED! in green when complete.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Text color setting
        this.textColorSetting = new ModuleSetting.ColorSetting(
            "text_color",
            "Text Color",
            "Color of the time text while smelting",
            0xFFFFFFFF
        );
        registerSetting(textColorSetting);
        
        // Finished color setting
        this.finishedColorSetting = new ModuleSetting.ColorSetting(
            "finished_color",
            "Finished Color",
            "Color of the FINISHED! text",
            0xFF00FF00
        );
        registerSetting(finishedColorSetting);
        
        // Finished display time
        this.finishedDisplayTimeSetting = new ModuleSetting.NumberSetting(
            "finished_display_time",
            "Finished Display Time",
            "How long to show FINISHED! message (seconds)",
            3,
            1,
            10
        );
        registerSetting(finishedDisplayTimeSetting);
        
        // Show background setting
        this.showBackgroundSetting = new ModuleSetting.BooleanSetting(
            "show_background",
            "Show Background",
            "Show a background behind the overlay text",
            true
        );
        registerSetting(showBackgroundSetting);
        
        // Overlay X position
        this.overlayXSetting = new ModuleSetting.NumberSetting(
            "overlay_x",
            "Overlay X",
            "X position of the overlay (from left)",
            10,
            0,
            1000
        );
        registerSetting(overlayXSetting);
        
        // Overlay Y position
        this.overlayYSetting = new ModuleSetting.NumberSetting(
            "overlay_y",
            "Overlay Y",
            "Y position of the overlay (from top)",
            10,
            0,
            1000
        );
        registerSetting(overlayYSetting);
    }
    
    /**
     * Creates the pin button for the furnace screen.
     */
    public void createButton(HandledScreen<?> screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        if (!EstimatedFuelTimeModule.isFurnaceScreen(screen)) {
            return;
        }
        
        WidgetSettings settings = getWidgetSettings(PIN_TIME_WIDGET_ID, 20, -20);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        // Check if this furnace is already pinned
        boolean isPinned = isCurrentFurnacePinned();
        
        pinButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            isPinned ? "📍" : "📌",
            isPinned ? "Unpin Time" : "Pin Time",
            isPinned ? 
                "Unpin the estimated time overlay.\n" +
                "Click to stop tracking this furnace." :
                "Pin the estimated time as an overlay.\n" +
                "Shows even after closing the furnace.",
            button -> togglePinCurrentFurnace(screen)
        );
        
        // Apply display mode
        pinButton.setDisplayMode(settings.getDisplayMode());
        
        pinButton.setEditorInfo(this, PIN_TIME_WIDGET_ID, "Pin Time", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(pinButton);
        
        addDrawableChild.accept(pinButton);
    }
    
    /**
     * Checks if the current furnace (based on crosshair) is already pinned.
     */
    private boolean isCurrentFurnacePinned() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult blockHit) {
            return trackedFurnaces.containsKey(blockHit.getBlockPos());
        }
        return false;
    }
    
    /**
     * Gets the position of the current furnace (based on crosshair).
     */
    private BlockPos getCurrentFurnacePos() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult blockHit) {
            return blockHit.getBlockPos();
        }
        return null;
    }
    
    /**
     * Toggles pinning for the current furnace.
     */
    private void togglePinCurrentFurnace(HandledScreen<?> screen) {
        BlockPos furnacePos = getCurrentFurnacePos();
        if (furnacePos == null) return;
        
        if (trackedFurnaces.containsKey(furnacePos)) {
            // Already pinned - unpin it
            trackedFurnaces.remove(furnacePos);
            updateButtonState(false);
        } else {
            // Not pinned - pin it
            pinFurnace(screen, furnacePos);
            updateButtonState(true);
        }
    }
    
    /**
     * Updates the button appearance based on pin state.
     */
    private void updateButtonState(boolean isPinned) {
        if (pinButton != null) {
            pinButton.setMessage(net.minecraft.text.Text.literal(isPinned ? "📍" : "📌"));
        }
    }
    
    /**
     * Pins a furnace at the specified position.
     */
    private void pinFurnace(HandledScreen<?> screen, BlockPos furnacePos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        if (screen.getScreenHandler() instanceof AbstractFurnaceScreenHandler handler) {
            // Calculate estimated completion time
            long currentTime = System.currentTimeMillis();
            int remainingTicks = calculateRemainingTicks(handler);
            long estimatedCompletionTime = currentTime + (remainingTicks * 50L); // 50ms per tick
            
            String furnaceName = getFurnaceName(handler);
            
            // Get current slot counts
            int inputCount = handler.getSlot(0).getStack().getCount();
            int fuelCount = handler.getSlot(1).getStack().getCount();
            int outputCount = handler.getSlot(2).getStack().getCount();
            
            TrackedFurnace tracked = new TrackedFurnace(
                furnacePos,
                furnaceName,
                estimatedCompletionTime,
                remainingTicks > 0,
                inputCount,
                fuelCount,
                outputCount
            );
            
            trackedFurnaces.put(furnacePos, tracked);
        }
    }
    
    /**
     * Pins the current furnace's smelting time.
     * @deprecated Use togglePinCurrentFurnace instead
     */
    @Deprecated
    private void pinCurrentFurnace(HandledScreen<?> screen) {
        togglePinCurrentFurnace(screen);
    }
    
    /**
     * Calculates remaining ticks for current smelting operation.
     */
    private int calculateRemainingTicks(AbstractFurnaceScreenHandler handler) {
        int cookProgress = (int) handler.getCookProgress();
        int maxProgress = 24;
        int remainingProgressForCurrent = maxProgress - cookProgress;
        
        int ticksPerItem = getTotalCookTime(handler);
        float progressRatio = (float) remainingProgressForCurrent / maxProgress;
        int ticksForCurrentItem = (int) (ticksPerItem * progressRatio);
        
        int inputCount = handler.getSlot(0).getStack().getCount();
        int ticksForRemainingItems = (inputCount > 0 ? (inputCount - 1) : 0) * ticksPerItem;
        
        return ticksForCurrentItem + ticksForRemainingItems;
    }
    
    /**
     * Gets the total cook time based on furnace type.
     */
    private int getTotalCookTime(AbstractFurnaceScreenHandler handler) {
        String className = handler.getClass().getSimpleName();
        if (className.contains("Blast") || className.contains("Smoker")) {
            return 100;
        }
        return 200;
    }
    
    /**
     * Gets a display name for the furnace type.
     */
    private String getFurnaceName(AbstractFurnaceScreenHandler handler) {
        String className = handler.getClass().getSimpleName();
        if (className.contains("Blast")) {
            return "Blast Furnace";
        } else if (className.contains("Smoker")) {
            return "Smoker";
        }
        return "Furnace";
    }
    
    /**
     * Renders all pinned furnace overlays.
     * Should be called from the HUD render callback.
     */
    public void renderOverlays(DrawContext context) {
        if (!isEnabled() || trackedFurnaces.isEmpty()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        
        int x = overlayXSetting.getValue().intValue();
        int y = overlayYSetting.getValue().intValue();
        
        long currentTime = System.currentTimeMillis();
        int finishedDisplayMs = finishedDisplayTimeSetting.getValue().intValue() * 1000;
        
        // Clean up expired entries and render
        Iterator<Map.Entry<BlockPos, TrackedFurnace>> iterator = trackedFurnaces.entrySet().iterator();
        int yOffset = 0;
        
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, TrackedFurnace> entry = iterator.next();
            TrackedFurnace furnace = entry.getValue();
            
            long timeRemaining = furnace.estimatedCompletionTime - currentTime;
            boolean isFinished = timeRemaining <= 0;
            
            if (isFinished) {
                // Check if we should remove it
                long timeSinceFinished = -timeRemaining;
                if (timeSinceFinished > finishedDisplayMs) {
                    iterator.remove();
                    continue;
                }
                
                // Show FINISHED! text
                String text = furnace.furnaceName + ": FINISHED!";
                renderOverlayText(context, textRenderer, text, x, y + yOffset, finishedColorSetting.getValue());
            } else {
                // Show remaining time
                float seconds = timeRemaining / 1000.0f;
                String timeText;
                if (seconds < 60) {
                    timeText = String.format("%.1fs", seconds);
                } else {
                    int mins = (int) (seconds / 60);
                    float secs = seconds % 60;
                    timeText = String.format("%dm %.0fs", mins, secs);
                }
                
                String text = furnace.furnaceName + ": " + timeText;
                renderOverlayText(context, textRenderer, text, x, y + yOffset, textColorSetting.getValue());
            }
            
            yOffset += 12;
        }
    }
    
    /**
     * Renders a single overlay text line.
     */
    private void renderOverlayText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
        int textWidth = textRenderer.getWidth(text);
        
        if (showBackgroundSetting.getValue()) {
            context.fill(x - 2, y - 2, x + textWidth + 4, y + 10, 0xAA000000);
        }
        
        context.drawTextWithShadow(textRenderer, text, x, y, color);
    }
    
    /**
     * Updates tracked furnaces when viewing a furnace screen.
     * Only updates when there's a significant change in items to avoid timer freezing.
     */
    public void updateTrackedFurnace(BlockPos pos, AbstractFurnaceScreenHandler handler) {
        if (trackedFurnaces.containsKey(pos)) {
            TrackedFurnace tracked = trackedFurnaces.get(pos);
            
            // Get current item count to detect changes
            int currentInputCount = handler.getSlot(0).getStack().getCount();
            int currentFuelCount = handler.getSlot(1).getStack().getCount();
            int currentOutputCount = handler.getSlot(2).getStack().getCount();
            
            // Only recalculate if items changed (user moved items in/out)
            if (currentInputCount != tracked.lastInputCount ||
                currentFuelCount != tracked.lastFuelCount ||
                currentOutputCount != tracked.lastOutputCount) {
                
                long currentTime = System.currentTimeMillis();
                int remainingTicks = calculateRemainingTicks(handler);
                
                if (remainingTicks > 0) {
                    tracked.estimatedCompletionTime = currentTime + (remainingTicks * 50L);
                    tracked.isProcessing = true;
                } else {
                    // No longer processing
                    if (tracked.isProcessing) {
                        tracked.isProcessing = false;
                        tracked.estimatedCompletionTime = currentTime; // Mark as finished now
                    }
                }
                
                // Update cached counts
                tracked.lastInputCount = currentInputCount;
                tracked.lastFuelCount = currentFuelCount;
                tracked.lastOutputCount = currentOutputCount;
            }
        }
    }
    
    /**
     * Removes a tracked furnace.
     */
    public static void untrackFurnace(BlockPos pos) {
        trackedFurnaces.remove(pos);
    }
    
    /**
     * Clears all tracked furnaces.
     */
    public static void clearTracked() {
        trackedFurnaces.clear();
    }
    
    /**
     * Returns whether there are any active overlays.
     */
    public boolean hasActiveOverlays() {
        return !trackedFurnaces.isEmpty();
    }
    
    @Override
    public Set<String> getWidgetIds() {
        Set<String> ids = new HashSet<>();
        ids.add(PIN_TIME_WIDGET_ID);
        return ids;
    }
    
    /**
     * Tracked furnace data.
     */
    private static class TrackedFurnace {
        BlockPos position;
        String furnaceName;
        long estimatedCompletionTime;
        boolean isProcessing;
        // Cached slot counts to detect changes
        int lastInputCount;
        int lastFuelCount;
        int lastOutputCount;
        
        TrackedFurnace(BlockPos position, String furnaceName, long estimatedCompletionTime, boolean isProcessing,
                       int inputCount, int fuelCount, int outputCount) {
            this.position = position;
            this.furnaceName = furnaceName;
            this.estimatedCompletionTime = estimatedCompletionTime;
            this.isProcessing = isProcessing;
            this.lastInputCount = inputCount;
            this.lastFuelCount = fuelCount;
            this.lastOutputCount = outputCount;
        }
    }
}
