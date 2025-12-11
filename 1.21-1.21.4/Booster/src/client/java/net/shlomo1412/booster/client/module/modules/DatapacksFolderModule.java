package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Module that adds an "Open Datapacks Folder" button to the singleplayer pause menu.
 * Opens the current world's datapacks folder in the system file explorer.
 */
public class DatapacksFolderModule extends GUIModule {
    
    public static final String DATAPACKS_FOLDER_WIDGET_ID = "datapacks_folder";
    
    private BoosterButton datapacksButton;
    
    public DatapacksFolderModule() {
        super(
            "datapacks_folder",
            "Datapacks Folder",
            "Adds a button to open the world's datapacks folder.\n" +
            "Singleplayer only.",
            true,
            20,  // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Creates the Datapacks Folder button for the pause menu.
     */
    public void createButton(GameMenuScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(DATAPACKS_FOLDER_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        datapacksButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "📦",
            "Datapacks",
            "Open the world's datapacks folder in file explorer.",
            button -> openDatapacksFolder()
        );
        
        // Apply display mode from settings
        datapacksButton.setDisplayMode(settings.getDisplayMode());
        
        datapacksButton.setEditorInfo(this, DATAPACKS_FOLDER_WIDGET_ID, "Datapacks", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(datapacksButton);
        
        addDrawableChild.accept(datapacksButton);
    }
    
    /**
     * Opens the datapacks folder in the file explorer.
     */
    private void openDatapacksFolder() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.isInSingleplayer() && client.getServer() != null) {
            try {
                // Get the datapacks path from the server
                Path datapacksPath = client.getServer().getSavePath(WorldSavePath.DATAPACKS);
                File datapacksDir = datapacksPath.toFile();
                
                // Create the folder if it doesn't exist
                if (!datapacksDir.exists()) {
                    datapacksDir.mkdirs();
                }
                
                Util.getOperatingSystem().open(datapacksDir);
            } catch (Exception e) {
                BoosterClient.LOGGER.error("Failed to open datapacks folder", e);
            }
        }
    }
    
    /**
     * Gets the button.
     */
    public BoosterButton getButton() {
        return datapacksButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        datapacksButton = null;
    }
}
