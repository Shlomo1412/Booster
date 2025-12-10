package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds a "Save & Quit to Servers" button to the multiplayer pause menu.
 * Disconnects and goes directly to the server selection screen.
 */
public class SaveQuitToServersModule extends GUIModule {
    
    public static final String SAVE_QUIT_SERVERS_WIDGET_ID = "save_quit_to_servers";
    
    private BoosterButton saveQuitButton;
    
    public SaveQuitToServersModule() {
        super(
            "save_quit_to_servers",
            "Quit to Servers",
            "Adds a button to the multiplayer pause menu to disconnect and go to server selection.",
            true,
            130, // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Creates the Quit to Servers button for the multiplayer pause menu.
     */
    public void createButton(GameMenuScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(SAVE_QUIT_SERVERS_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        saveQuitButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🖧 Quit to Servers",
            "Disconnect & Go to Server List",
            "Disconnects from the server and returns to the server selection screen.\n" +
            "Skip the title screen and quickly switch servers!",
            button -> {
                quitToServers();
            }
        );
        
        saveQuitButton.setEditorInfo(this, SAVE_QUIT_SERVERS_WIDGET_ID, "Quit to Servers", anchorX, anchorY);
        
        addDrawableChild.accept(saveQuitButton);
    }
    
    /**
     * Disconnects and goes to server selection.
     */
    private void quitToServers() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.world != null && !client.isInSingleplayer()) {
            // Disconnect from the server
            client.world.disconnect();
            client.disconnect();
            
            // Go directly to server selection screen
            client.setScreen(new MultiplayerScreen(new TitleScreen()));
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
