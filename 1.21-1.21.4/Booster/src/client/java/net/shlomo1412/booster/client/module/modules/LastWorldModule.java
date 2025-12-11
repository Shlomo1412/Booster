package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.List;
import java.util.function.Consumer;

/**
 * Module that adds a "Last World" button to the title screen.
 * Allows quickly rejoining the last played singleplayer world.
 */
public class LastWorldModule extends GUIModule {
    
    public static final String LAST_WORLD_WIDGET_ID = "last_world";
    
    // Store last world info
    private static String lastWorldName = null;
    private static String lastWorldDisplayName = null;
    
    private BoosterButton lastWorldButton;
    
    public LastWorldModule() {
        super(
            "last_world",
            "Last World",
            "Adds a button to the title screen to quickly rejoin the last played world.",
            true,
            100, // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Records the last world that was played.
     */
    public static void setLastWorld(String worldName, String displayName) {
        lastWorldName = worldName;
        lastWorldDisplayName = displayName;
        // Save to config
        ModuleManager.getInstance().getConfig().setLastWorldName(worldName);
        ModuleManager.getInstance().getConfig().setLastWorldDisplayName(displayName);
        ModuleManager.getInstance().saveConfig();
    }
    
    /**
     * Gets the last world folder name.
     */
    public static String getLastWorldName() {
        if (lastWorldName == null) {
            lastWorldName = ModuleManager.getInstance().getConfig().getLastWorldName();
        }
        return lastWorldName;
    }
    
    /**
     * Gets the last world display name.
     */
    public static String getLastWorldDisplayName() {
        if (lastWorldDisplayName == null) {
            lastWorldDisplayName = ModuleManager.getInstance().getConfig().getLastWorldDisplayName();
        }
        return lastWorldDisplayName;
    }
    
    /**
     * Checks if there's a last world to load.
     */
    public static boolean hasLastWorld() {
        return getLastWorldName() != null && !getLastWorldName().isEmpty();
    }
    
    /**
     * Creates the Last World button for the title screen.
     */
    public void createButton(TitleScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(LAST_WORLD_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        String displayName = getLastWorldDisplayName();
        String buttonName = hasLastWorld() ? 
            (displayName != null ? displayName : "Last World") : 
            "No Recent World";
        
        lastWorldButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🌍",
            buttonName,
            hasLastWorld() ? 
                "Quickly load: " + (displayName != null ? displayName : getLastWorldName()) :
                "No world history yet. Play a world first!",
            button -> {
                if (hasLastWorld()) {
                    loadWorld(screen);
                }
            }
        );
        
        // Apply display mode from settings
        lastWorldButton.setDisplayMode(settings.getDisplayMode());
        
        lastWorldButton.active = hasLastWorld();
        lastWorldButton.setEditorInfo(this, LAST_WORLD_WIDGET_ID, "Last World", anchorX, anchorY);
        
        addDrawableChild.accept(lastWorldButton);
    }
    
    /**
     * Loads the last world.
     */
    private void loadWorld(TitleScreen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        String worldName = getLastWorldName();
        
        if (worldName != null && !worldName.isEmpty()) {
            // Use the level storage to start the world
            LevelStorage levelStorage = client.getLevelStorage();
            
            try {
                // Create a session for the world and start it
                if (levelStorage.levelExists(worldName)) {
                    // Go through SelectWorldScreen to properly load the world
                    // This handles all the edge cases (world version, etc.)
                    client.createIntegratedServerLoader().start(worldName, () -> {
                        client.setScreen(new SelectWorldScreen(screen));
                    });
                }
            } catch (Exception e) {
                net.shlomo1412.booster.client.BoosterClient.LOGGER.error("Failed to load last world: " + worldName, e);
            }
        }
    }
    
    /**
     * Gets the button created by this module.
     */
    public BoosterButton getButton() {
        return lastWorldButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        lastWorldButton = null;
    }
}
