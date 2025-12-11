package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Module that adds a "Server Info" button to the multiplayer pause menu.
 * Shows server information in a tooltip or modal.
 */
public class ServerInfoModule extends GUIModule {
    
    public static final String SERVER_INFO_WIDGET_ID = "server_info";
    
    private BoosterButton serverInfoButton;
    private boolean showingInfo = false;
    
    public ServerInfoModule() {
        super(
            "server_info",
            "Server Info",
            "Adds a button to show server information.\n" +
            "Multiplayer only.",
            true,
            20,  // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Creates the Server Info button for the pause menu.
     */
    public void createButton(GameMenuScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(SERVER_INFO_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        // Build tooltip with server info
        String tooltip = buildServerInfoTooltip();
        
        serverInfoButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "ℹ️",
            "Server Info",
            tooltip,
            button -> toggleInfoPanel()
        );
        
        // Apply display mode from settings
        serverInfoButton.setDisplayMode(settings.getDisplayMode());
        
        serverInfoButton.setEditorInfo(this, SERVER_INFO_WIDGET_ID, "Server Info", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(serverInfoButton);
        
        addDrawableChild.accept(serverInfoButton);
    }
    
    /**
     * Builds the tooltip text with server information.
     */
    private String buildServerInfoTooltip() {
        MinecraftClient client = MinecraftClient.getInstance();
        StringBuilder sb = new StringBuilder();
        
        sb.append("§6§lServer Information\n\n");
        
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            sb.append("§7Name: §f").append(serverInfo.name).append("\n");
            sb.append("§7Address: §f").append(serverInfo.address).append("\n");
        }
        
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler != null) {
            // Player count
            Collection<PlayerListEntry> players = networkHandler.getPlayerList();
            sb.append("§7Players: §f").append(players.size()).append("\n");
            
            // Server brand
            String brand = networkHandler.getBrand();
            if (brand != null) {
                sb.append("§7Server: §f").append(brand).append("\n");
            }
            
            // Connection info
            if (client.getNetworkHandler() != null && client.getNetworkHandler().getConnection() != null) {
                sb.append("\n§6§lConnection Info\n");
                var connection = client.getNetworkHandler().getConnection();
                sb.append("§7Protocol: §f").append(connection.isEncrypted() ? "Encrypted" : "Unencrypted").append("\n");
            }
        }
        
        // World info
        if (client.world != null) {
            sb.append("\n§6§lWorld Info\n");
            sb.append("§7Difficulty: §f").append(client.world.getDifficulty().getName()).append("\n");
            
            // Day time
            long time = client.world.getTimeOfDay() % 24000;
            int hours = (int) ((time / 1000 + 6) % 24);
            int minutes = (int) ((time % 1000) * 60 / 1000);
            sb.append("§7Time: §f").append(String.format("%02d:%02d", hours, minutes)).append("\n");
            
            // Weather
            if (client.world.isThundering()) {
                sb.append("§7Weather: §fThunderstorm\n");
            } else if (client.world.isRaining()) {
                sb.append("§7Weather: §fRaining\n");
            } else {
                sb.append("§7Weather: §fClear\n");
            }
        }
        
        // Dev info
        if (client.player != null) {
            sb.append("\n§6§lDev Info\n");
            sb.append("§7Position: §f").append(String.format("%.1f, %.1f, %.1f", 
                client.player.getX(), client.player.getY(), client.player.getZ())).append("\n");
            sb.append("§7Dimension: §f").append(client.world.getRegistryKey().getValue().toString()).append("\n");
            sb.append("§7View Distance: §f").append(client.options.getViewDistance().getValue()).append(" chunks\n");
            
            // Ping
            PlayerListEntry entry = networkHandler.getPlayerListEntry(client.player.getUuid());
            if (entry != null) {
                sb.append("§7Ping: §f").append(entry.getLatency()).append("ms\n");
            }
        }
        
        sb.append("\n§8Hold CTRL for full info");
        
        return sb.toString();
    }
    
    /**
     * Toggles the info panel visibility.
     */
    private void toggleInfoPanel() {
        showingInfo = !showingInfo;
    }
    
    /**
     * Renders the info panel if visible.
     */
    public void renderInfoPanel(DrawContext context, int mouseX, int mouseY) {
        if (!showingInfo || serverInfoButton == null) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        int panelWidth = 200;
        int panelHeight = 220;
        int x = serverInfoButton.getX() - panelWidth - 5;
        int y = serverInfoButton.getY();
        
        // Keep panel on screen
        if (x < 5) x = serverInfoButton.getX() + serverInfoButton.getWidth() + 5;
        if (y + panelHeight > client.getWindow().getScaledHeight() - 5) {
            y = client.getWindow().getScaledHeight() - panelHeight - 5;
        }
        
        // Background
        context.fill(x, y, x + panelWidth, y + panelHeight, 0xE0202020);
        context.drawBorder(x, y, panelWidth, panelHeight, 0xFF404040);
        
        // Title
        context.drawCenteredTextWithShadow(client.textRenderer, "§6§lServer Information", 
            x + panelWidth / 2, y + 5, 0xFFFFFF);
        
        int textY = y + 20;
        int lineHeight = 11;
        
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            context.drawTextWithShadow(client.textRenderer, "§7Name: §f" + serverInfo.name, x + 5, textY, 0xFFFFFF);
            textY += lineHeight;
            context.drawTextWithShadow(client.textRenderer, "§7Address: §f" + serverInfo.address, x + 5, textY, 0xFFFFFF);
            textY += lineHeight;
        }
        
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler != null) {
            Collection<PlayerListEntry> players = networkHandler.getPlayerList();
            context.drawTextWithShadow(client.textRenderer, "§7Players: §f" + players.size(), x + 5, textY, 0xFFFFFF);
            textY += lineHeight;
            
            String brand = networkHandler.getBrand();
            if (brand != null) {
                context.drawTextWithShadow(client.textRenderer, "§7Server: §f" + brand, x + 5, textY, 0xFFFFFF);
                textY += lineHeight;
            }
        }
        
        // Dev section
        textY += 5;
        context.drawTextWithShadow(client.textRenderer, "§6Dev Info", x + 5, textY, 0xFFFFFF);
        textY += lineHeight;
        
        if (client.player != null && client.world != null) {
            context.drawTextWithShadow(client.textRenderer, 
                String.format("§7Pos: §f%.0f, %.0f, %.0f", 
                    client.player.getX(), client.player.getY(), client.player.getZ()), 
                x + 5, textY, 0xFFFFFF);
            textY += lineHeight;
            
            String dim = client.world.getRegistryKey().getValue().getPath();
            context.drawTextWithShadow(client.textRenderer, "§7Dim: §f" + dim, x + 5, textY, 0xFFFFFF);
            textY += lineHeight;
            
            if (networkHandler != null) {
                PlayerListEntry entry = networkHandler.getPlayerListEntry(client.player.getUuid());
                if (entry != null) {
                    context.drawTextWithShadow(client.textRenderer, "§7Ping: §f" + entry.getLatency() + "ms", 
                        x + 5, textY, 0xFFFFFF);
                }
            }
        }
    }
    
    /**
     * Handles click to close the panel.
     */
    public boolean handleClick(double mouseX, double mouseY) {
        if (showingInfo && serverInfoButton != null) {
            // Check if click is outside the panel
            int panelWidth = 200;
            int panelHeight = 220;
            int x = serverInfoButton.getX() - panelWidth - 5;
            int y = serverInfoButton.getY();
            
            if (mouseX < x || mouseX > x + panelWidth || mouseY < y || mouseY > y + panelHeight) {
                showingInfo = false;
                return true;
            }
        }
        return false;
    }
    
    /**
     * @return Whether the info panel is showing
     */
    public boolean isShowingInfo() {
        return showingInfo;
    }
    
    /**
     * Closes the info panel.
     */
    public void closeInfo() {
        showingInfo = false;
    }
    
    /**
     * Gets the button.
     */
    public BoosterButton getButton() {
        return serverInfoButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        serverInfoButton = null;
        showingInfo = false;
    }
}
