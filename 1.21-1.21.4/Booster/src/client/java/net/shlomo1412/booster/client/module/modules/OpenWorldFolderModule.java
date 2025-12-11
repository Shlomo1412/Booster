package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelStorage;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.io.File;
import java.util.function.Consumer;

/**
 * Module that adds an "Open World Folder" button to the singleplayer pause menu.
 * Opens the current world's save folder in the system file explorer.
 */
public class OpenWorldFolderModule extends GUIModule {
    
    public static final String WORLD_FOLDER_WIDGET_ID = "open_world_folder";
    
    private BoosterButton worldFolderButton;
    
    public OpenWorldFolderModule() {
        super(
            "open_world_folder",
            "Open World Folder",
            "Adds a button to open the current world's folder.\n" +
            "Singleplayer only.",
            true,
            20,  // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Creates the Open World Folder button for the pause menu.
     */
    public void createButton(GameMenuScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(WORLD_FOLDER_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        worldFolderButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "📁",
            "World Folder",
            "Open the current world's save folder in file explorer.",
            button -> openWorldFolder()
        );
        
        // Apply display mode from settings
        worldFolderButton.setDisplayMode(settings.getDisplayMode());
        
        worldFolderButton.setEditorInfo(this, WORLD_FOLDER_WIDGET_ID, "World Folder", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(worldFolderButton);
        
        addDrawableChild.accept(worldFolderButton);
    }
    
    /**
     * Opens the current world's folder in the file explorer.
     */
    private void openWorldFolder() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.isInSingleplayer() && client.getServer() != null) {
            try {
                // Get the world save path
                LevelStorage.Session session = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT).getParent().toFile().getParentFile() != null ? null : null;
                
                // Alternative: use the saves directory + world name
                File savesDir = client.getLevelStorage().getSavesDirectory().toFile();
                String worldName = client.getServer().getSaveProperties().getLevelName();
                
                // Get world folder name from the integrated server
                String worldFolder = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT).getParent().getFileName().toString();
                File worldDir = new File(savesDir, worldFolder);
                
                if (worldDir.exists() && worldDir.isDirectory()) {
                    Util.getOperatingSystem().open(worldDir);
                } else {
                    BoosterClient.LOGGER.warn("World folder not found: {}", worldDir.getAbsolutePath());
                }
            } catch (Exception e) {
                BoosterClient.LOGGER.error("Failed to open world folder", e);
            }
        }
    }
    
    /**
     * Gets the button.
     */
    public BoosterButton getButton() {
        return worldFolderButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        worldFolderButton = null;
    }
}
