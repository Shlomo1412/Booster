package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.screen.ScreenshotViewerScreen;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds a "Screenshot Viewer" button to the pause menu.
 * Opens a full-featured screenshot viewer with gallery, zoom, and editing tools.
 */
public class ScreenshotViewerModule extends GUIModule {
    
    public static final String SCREENSHOT_VIEWER_WIDGET_ID = "screenshot_viewer";
    
    private BoosterButton viewerButton;
    
    public ScreenshotViewerModule() {
        super(
            "screenshot_viewer",
            "Screenshot Viewer",
            "Adds a button to the pause menu to open an advanced screenshot viewer.\n" +
            "Features:\n" +
            "• Gallery view with thumbnails\n" +
            "• Full-screen preview with zoom and pan\n" +
            "• Copy, rename, and delete screenshots\n" +
            "• Search and sort functionality\n" +
            "• Keyboard navigation",
            true,
            20,  // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Creates the Screenshot Viewer button for the pause menu.
     */
    public void createButton(GameMenuScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(SCREENSHOT_VIEWER_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        viewerButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🖼️",
            "Screenshot Viewer",
            "Opens the screenshot viewer with gallery view.\n" +
            "View, zoom, copy, rename, and delete your screenshots!\n\n" +
            "§7Shortcuts in viewer:\n" +
            "§8• Arrow keys: Navigate\n" +
            "§8• Enter: Preview\n" +
            "§8• +/-: Zoom\n" +
            "§8• Del: Delete",
            button -> openScreenshotViewer(screen)
        );
        
        // Apply display mode from settings
        viewerButton.setDisplayMode(settings.getDisplayMode());
        
        viewerButton.setEditorInfo(this, SCREENSHOT_VIEWER_WIDGET_ID, "Screenshot Viewer", anchorX, anchorY);
        
        addDrawableChild.accept(viewerButton);
    }
    
    /**
     * Opens the screenshot viewer screen.
     */
    private void openScreenshotViewer(GameMenuScreen currentScreen) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ScreenshotViewerScreen(currentScreen));
    }
    
    /**
     * Gets the button created by this module.
     */
    public BoosterButton getButton() {
        return viewerButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        viewerButton = null;
    }
}
