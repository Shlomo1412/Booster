package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds a "Save & Quit to Worlds" button to the singleplayer pause menu.
 * Saves the game and goes directly to the world selection screen.
 */
public class SaveQuitToWorldsModule extends GUIModule {
    
    public static final String SAVE_QUIT_WORLDS_WIDGET_ID = "save_quit_to_worlds";
    
    private BoosterButton saveQuitButton;
    
    public SaveQuitToWorldsModule() {
        super(
            "save_quit_to_worlds",
            "Save & Quit to Worlds",
            "Adds a button to the singleplayer pause menu to save and go to world selection.",
            true,
            140, // Default button width (wider for text)
            20   // Default button height
        );
    }
    
    /**
     * Creates the Save & Quit to Worlds button for the singleplayer pause menu.
     */
    public void createButton(GameMenuScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(SAVE_QUIT_WORLDS_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        saveQuitButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🌍 Save & Quit to Worlds",
            "Save & Go to World Selection",
            "Saves your progress and returns to the world selection screen.\n" +
            "Skip the title screen and quickly switch worlds!",
            button -> {
                saveAndQuitToWorlds();
            }
        );
        
        saveQuitButton.setEditorInfo(this, SAVE_QUIT_WORLDS_WIDGET_ID, "Save & Quit to Worlds", anchorX, anchorY);
        
        addDrawableChild.accept(saveQuitButton);
    }
    
    /**
     * Saves the game and goes to world selection.
     */
    private void saveAndQuitToWorlds() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.world != null && client.isInSingleplayer()) {
            // Disconnect from the world
            client.world.disconnect();
            client.disconnect();
            
            // Go directly to world selection screen
            client.setScreen(new SelectWorldScreen(new TitleScreen()));
        }
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
