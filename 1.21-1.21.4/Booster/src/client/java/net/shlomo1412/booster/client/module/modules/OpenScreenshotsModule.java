package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.util.Util;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.io.File;
import java.util.function.Consumer;

/**
 * Module that adds an "Open Screenshots" button to the pause menu.
 * Opens the screenshots folder in the system file explorer.
 */
public class OpenScreenshotsModule extends GUIModule {
    
    public static final String OPEN_SCREENSHOTS_WIDGET_ID = "open_screenshots";
    
    private BoosterButton screenshotsButton;
    
    public OpenScreenshotsModule() {
        super(
            "open_screenshots",
            "Open Screenshots",
            "Adds a button to the pause menu to open the screenshots folder.",
            true,
            20,  // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Creates the Open Screenshots button for the pause menu.
     */
    public void createButton(GameMenuScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(OPEN_SCREENSHOTS_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        screenshotsButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "📷",
            "Screenshots",
            "Opens your screenshots folder in the file explorer.\n" +
            "Quickly access all your Minecraft screenshots!",
            button -> openScreenshotsFolder()
        );
        
        // Apply display mode from settings
        screenshotsButton.setDisplayMode(settings.getDisplayMode());
        
        screenshotsButton.setEditorInfo(this, OPEN_SCREENSHOTS_WIDGET_ID, "Open Screenshots", anchorX, anchorY);
        
        addDrawableChild.accept(screenshotsButton);
    }
    
    /**
     * Opens the screenshots folder in the system file explorer.
     */
    private void openScreenshotsFolder() {
        MinecraftClient client = MinecraftClient.getInstance();
        File screenshotsDir = new File(client.runDirectory, "screenshots");
        
        // Create the folder if it doesn't exist
        if (!screenshotsDir.exists()) {
            screenshotsDir.mkdirs();
        }
        
        // Open the folder using the system's default file explorer
        Util.getOperatingSystem().open(screenshotsDir);
    }
    
    /**
     * Gets the button created by this module.
     */
    public BoosterButton getButton() {
        return screenshotsButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        screenshotsButton = null;
    }
}
