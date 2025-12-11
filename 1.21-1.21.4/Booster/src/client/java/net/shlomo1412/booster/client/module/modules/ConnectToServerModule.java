package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Module that adds a "Connect to Server" dropdown button to the multiplayer pause menu.
 * Shows a list of recent/saved servers that can be connected to instantly.
 */
public class ConnectToServerModule extends GUIModule {
    
    public static final String CONNECT_SERVER_WIDGET_ID = "connect_to_server";
    
    private final ModuleSetting.NumberSetting maxServersSetting;
    
    private BoosterButton connectServerButton;
    private boolean dropdownOpen = false;
    private List<ServerEntry> recentServers = new ArrayList<>();
    
    public ConnectToServerModule() {
        super(
            "connect_to_server",
            "Connect to Server",
            "Adds a dropdown to quickly connect to another server.\n" +
            "Shows saved servers for instant connection.\n" +
            "Multiplayer only.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Configurable number of servers to show
        this.maxServersSetting = new ModuleSetting.NumberSetting(
            "max_servers",
            "Max Servers",
            "Maximum number of servers to show in the dropdown",
            5,   // default
            1,   // min
            15   // max
        );
        registerSetting(maxServersSetting);
    }
    
    /**
     * Creates the Connect to Server button for the pause menu.
     */
    public void createButton(GameMenuScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(CONNECT_SERVER_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        connectServerButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🌐",
            "Servers",
            "Click to show saved servers.\n" +
            "Quickly connect to another server.",
            button -> toggleDropdown()
        );
        
        // Apply display mode from settings
        connectServerButton.setDisplayMode(settings.getDisplayMode());
        
        connectServerButton.setEditorInfo(this, CONNECT_SERVER_WIDGET_ID, "Connect Server", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(connectServerButton);
        
        addDrawableChild.accept(connectServerButton);
        
        // Load servers
        loadServers();
    }
    
    /**
     * Toggles the dropdown visibility.
     */
    private void toggleDropdown() {
        dropdownOpen = !dropdownOpen;
        if (dropdownOpen) {
            loadServers();
        }
    }
    
    /**
     * Loads the list of saved servers.
     */
    private void loadServers() {
        MinecraftClient client = MinecraftClient.getInstance();
        recentServers.clear();
        
        try {
            ServerList serverList = new ServerList(client);
            serverList.loadFile();
            
            // Get current server to exclude
            String currentAddress = null;
            if (client.getCurrentServerEntry() != null) {
                currentAddress = client.getCurrentServerEntry().address;
            }
            
            int count = 0;
            for (int i = 0; i < serverList.size() && count < maxServersSetting.getValue(); i++) {
                ServerInfo info = serverList.get(i);
                // Exclude current server
                if (currentAddress != null && info.address.equals(currentAddress)) {
                    continue;
                }
                recentServers.add(new ServerEntry(info.name, info.address));
                count++;
            }
        } catch (Exception e) {
            BoosterClient.LOGGER.error("Failed to load server list", e);
        }
    }
    
    /**
     * Connects to the specified server.
     */
    private void connectToServer(ServerEntry server) {
        MinecraftClient client = MinecraftClient.getInstance();
        dropdownOpen = false;
        
        try {
            // Disconnect from current server
            if (client.world != null) {
                client.world.disconnect();
            }
            client.disconnect();
            
            // Connect to new server
            ServerAddress serverAddress = ServerAddress.parse(server.address);
            ServerInfo serverInfo = new ServerInfo(server.name, server.address, ServerInfo.ServerType.OTHER);
            
            ConnectScreen.connect(new TitleScreen(), client, serverAddress, serverInfo, false, null);
        } catch (Exception e) {
            BoosterClient.LOGGER.error("Failed to connect to server: " + server.address, e);
        }
    }
    
    /**
     * Renders the dropdown if open.
     */
    public void renderDropdown(DrawContext context, int mouseX, int mouseY) {
        if (!dropdownOpen || connectServerButton == null) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        int x = connectServerButton.getX();
        int y = connectServerButton.getY() + connectServerButton.getHeight() + 2;
        int width = 150;
        int itemHeight = 20;
        
        if (recentServers.isEmpty()) {
            // Show "No servers" message
            context.fill(x, y, x + width, y + itemHeight, 0xE0202020);
            context.drawBorder(x, y, width, itemHeight, 0xFF404040);
            context.drawCenteredTextWithShadow(client.textRenderer, "No other servers", x + width/2, y + 6, 0x888888);
            return;
        }
        
        int totalHeight = recentServers.size() * itemHeight;
        
        // Background
        context.fill(x, y, x + width, y + totalHeight, 0xE0202020);
        context.drawBorder(x, y, width, totalHeight, 0xFF404040);
        
        // Render each server entry
        for (int i = 0; i < recentServers.size(); i++) {
            ServerEntry server = recentServers.get(i);
            int itemY = y + i * itemHeight;
            
            // Highlight on hover
            boolean hovered = mouseX >= x && mouseX < x + width && 
                             mouseY >= itemY && mouseY < itemY + itemHeight;
            
            if (hovered) {
                context.fill(x + 1, itemY, x + width - 1, itemY + itemHeight, 0x60FFFFFF);
            }
            
            // Server name (truncated if too long)
            String displayName = server.name;
            if (client.textRenderer.getWidth(displayName) > width - 10) {
                while (client.textRenderer.getWidth(displayName + "...") > width - 10 && displayName.length() > 1) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                displayName += "...";
            }
            
            context.drawTextWithShadow(client.textRenderer, displayName, x + 5, itemY + 6, 0xFFFFFF);
        }
    }
    
    /**
     * Handles mouse click on the dropdown.
     * @return true if the click was handled
     */
    public boolean handleDropdownClick(double mouseX, double mouseY) {
        if (!dropdownOpen || connectServerButton == null) return false;
        
        int x = connectServerButton.getX();
        int y = connectServerButton.getY() + connectServerButton.getHeight() + 2;
        int width = 150;
        int itemHeight = 20;
        
        for (int i = 0; i < recentServers.size(); i++) {
            int itemY = y + i * itemHeight;
            
            if (mouseX >= x && mouseX < x + width && 
                mouseY >= itemY && mouseY < itemY + itemHeight) {
                connectToServer(recentServers.get(i));
                return true;
            }
        }
        
        // Click outside dropdown closes it
        dropdownOpen = false;
        return false;
    }
    
    /**
     * @return Whether the dropdown is currently open
     */
    public boolean isDropdownOpen() {
        return dropdownOpen;
    }
    
    /**
     * Closes the dropdown.
     */
    public void closeDropdown() {
        dropdownOpen = false;
    }
    
    /**
     * Gets the button.
     */
    public BoosterButton getButton() {
        return connectServerButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        connectServerButton = null;
        dropdownOpen = false;
    }
    
    /**
     * Simple record for server entries.
     */
    private record ServerEntry(String name, String address) {}
}
