package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds a "Reconnect" button to the multiplayer pause menu.
 * Disconnects and reconnects to the same server quickly.
 */
public class ReconnectModule extends GUIModule {
    
    public static final String RECONNECT_WIDGET_ID = "reconnect";
    
    private BoosterButton reconnectButton;
    
    public ReconnectModule() {
        super(
            "reconnect",
            "Reconnect",
            "Adds a button to quickly reconnect to the current server.\n" +
            "Multiplayer only.",
            true,
            20,  // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Creates the Reconnect button for the pause menu.
     */
    public void createButton(GameMenuScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(RECONNECT_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        reconnectButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🔄",
            "Reconnect",
            "Disconnect and reconnect to the current server.",
            button -> reconnect()
        );
        
        // Apply display mode from settings
        reconnectButton.setDisplayMode(settings.getDisplayMode());
        
        reconnectButton.setEditorInfo(this, RECONNECT_WIDGET_ID, "Reconnect", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(reconnectButton);
        
        addDrawableChild.accept(reconnectButton);
    }
    
    /**
     * Reconnects to the current server.
     */
    private void reconnect() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (!client.isInSingleplayer() && client.getCurrentServerEntry() != null) {
            ServerInfo serverInfo = client.getCurrentServerEntry();
            String address = serverInfo.address;
            String name = serverInfo.name;
            
            try {
                // Disconnect from current server
                client.world.disconnect();
                client.disconnect();
                
                // Reconnect to the same server
                ServerAddress serverAddress = ServerAddress.parse(address);
                ServerInfo newServerInfo = new ServerInfo(name, address, ServerInfo.ServerType.OTHER);
                
                ConnectScreen.connect(new TitleScreen(), client, serverAddress, newServerInfo, false, null);
            } catch (Exception e) {
                BoosterClient.LOGGER.error("Failed to reconnect to server", e);
            }
        }
    }
    
    /**
     * Gets the button.
     */
    public BoosterButton getButton() {
        return reconnectButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        reconnectButton = null;
    }
}
