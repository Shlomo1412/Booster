package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds a "Last Server" button to the title screen.
 * Allows quickly rejoining the last played server.
 */
public class LastServerModule extends GUIModule {
    
    public static final String LAST_SERVER_WIDGET_ID = "last_server";
    
    // Store last server info
    private static String lastServerName = null;
    private static String lastServerAddress = null;
    
    private BoosterButton lastServerButton;
    
    public LastServerModule() {
        super(
            "last_server",
            "Last Server",
            "Adds a button to the title screen to quickly rejoin the last played server.",
            true,
            100, // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Records the last server that was joined.
     */
    public static void setLastServer(String name, String address) {
        lastServerName = name;
        lastServerAddress = address;
        // Save to config
        ModuleManager.getInstance().getConfig().setLastServerName(name);
        ModuleManager.getInstance().getConfig().setLastServerAddress(address);
        ModuleManager.getInstance().saveConfig();
    }
    
    /**
     * Gets the last server name.
     */
    public static String getLastServerName() {
        if (lastServerName == null) {
            lastServerName = ModuleManager.getInstance().getConfig().getLastServerName();
        }
        return lastServerName;
    }
    
    /**
     * Gets the last server address.
     */
    public static String getLastServerAddress() {
        if (lastServerAddress == null) {
            lastServerAddress = ModuleManager.getInstance().getConfig().getLastServerAddress();
        }
        return lastServerAddress;
    }
    
    /**
     * Checks if there's a last server to connect to.
     */
    public static boolean hasLastServer() {
        return getLastServerAddress() != null && !getLastServerAddress().isEmpty();
    }
    
    /**
     * Creates the Last Server button for the title screen.
     */
    public void createButton(TitleScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(LAST_SERVER_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        String serverName = getLastServerName();
        String buttonName = hasLastServer() ? 
            (serverName != null ? serverName : "Last Server") : 
            "No Recent Server";
        
        lastServerButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "⚡",
            buttonName,
            hasLastServer() ? 
                "Quickly reconnect to: " + getLastServerAddress() :
                "No server history yet. Join a server first!",
            button -> {
                if (hasLastServer()) {
                    connectToServer(screen);
                }
            }
        );
        
        // Apply display mode from settings
        lastServerButton.setDisplayMode(settings.getDisplayMode());
        
        lastServerButton.active = hasLastServer();
        lastServerButton.setEditorInfo(this, LAST_SERVER_WIDGET_ID, "Last Server", anchorX, anchorY);
        
        addDrawableChild.accept(lastServerButton);
    }
    
    /**
     * Connects to the last server.
     */
    private void connectToServer(TitleScreen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        String address = getLastServerAddress();
        String name = getLastServerName();
        
        if (address != null && !address.isEmpty()) {
            ServerInfo serverInfo = new ServerInfo(
                name != null ? name : "Last Server",
                address,
                ServerInfo.ServerType.OTHER
            );
            
            ServerAddress serverAddress = ServerAddress.parse(address);
            ConnectScreen.connect(screen, client, serverAddress, serverInfo, false, null);
        }
    }
    
    /**
     * Gets the button created by this module.
     */
    public BoosterButton getButton() {
        return lastServerButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        lastServerButton = null;
    }
}
