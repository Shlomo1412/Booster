package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds a "Save & Quit Game" button to the pause menu.
 * Saves the game and exits Minecraft completely.
 */
public class SaveQuitGameModule extends GUIModule {
    
    public static final String SAVE_QUIT_GAME_WIDGET_ID = "save_quit_game";
    
    private BoosterButton saveQuitButton;
    
    public SaveQuitGameModule() {
        super(
            "save_quit_game",
            "Save & Quit Game",
            "Adds a button to the pause menu to save and exit the game completely.",
            true,
            120, // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Creates the Save & Quit Game button for the pause menu.
     */
    public void createButton(GameMenuScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(SAVE_QUIT_GAME_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        saveQuitButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "⏻ Save & Quit Game",
            "Save & Exit Game",
            "Saves your progress and exits Minecraft completely.\n" +
            "Perfect for quickly closing the game.",
            button -> {
                saveAndQuitGame();
            }
        );
        
        saveQuitButton.setEditorInfo(this, SAVE_QUIT_GAME_WIDGET_ID, "Save & Quit Game", anchorX, anchorY);
        
        addDrawableChild.accept(saveQuitButton);
    }
    
    /**
     * Saves the game and exits completely.
     */
    private void saveAndQuitGame() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Disconnect from server or save & close world
        if (client.world != null) {
            boolean singleplayer = client.isInSingleplayer();
            
            // Disconnect/close world
            client.world.disconnect();
            
            if (singleplayer) {
                // Wait for integrated server to stop
                client.disconnect();
            } else {
                client.disconnect();
            }
        }
        
        // Schedule shutdown on next tick to ensure clean disconnect
        client.scheduleStop();
    }
    
    /**
     * Gets the button created by this module.
     */
    public BoosterButton getButton() {
        return saveQuitButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        saveQuitButton = null;
    }
}
