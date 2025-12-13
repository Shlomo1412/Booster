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
        int panelWidth = 220;
        int padding = 8;
        int lineHeight = 12;
        int sectionGap = 6;
        
        // Calculate dynamic height based on content
        int contentHeight = calculatePanelHeight(client, lineHeight, sectionGap);
        int panelHeight = contentHeight + padding * 2;
        
        int x = serverInfoButton.getX() - panelWidth - 8;
        int y = serverInfoButton.getY();
        
        // Keep panel on screen
        if (x < 5) x = serverInfoButton.getX() + serverInfoButton.getWidth() + 8;
        if (y + panelHeight > client.getWindow().getScaledHeight() - 5) {
            y = client.getWindow().getScaledHeight() - panelHeight - 5;
        }
        
        // Main background with gradient effect (darker at edges)
        context.fill(x, y, x + panelWidth, y + panelHeight, 0xF0181818);
        
        // Inner highlight border
        context.fill(x + 1, y + 1, x + panelWidth - 1, y + 2, 0xFF3A3A3A);
        context.fill(x + 1, y + 1, x + 2, y + panelHeight - 1, 0xFF3A3A3A);
        
        // Outer border
        context.drawBorder(x, y, panelWidth, panelHeight, 0xFF505050);
        
        // Title bar background
        context.fill(x + 1, y + 1, x + panelWidth - 1, y + 18, 0xFF252525);
        context.fill(x + 1, y + 18, x + panelWidth - 1, y + 19, 0xFF404040);
        
        // Title with icon
        context.drawCenteredTextWithShadow(client.textRenderer, "§6⚡ §e§lServer Info §6⚡", 
            x + panelWidth / 2, y + 5, 0xFFFFFF);
        
        int textY = y + 24;
        int textX = x + padding;
        int valueX = x + 70;
        
        // Server section
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            // Server name row
            context.drawTextWithShadow(client.textRenderer, "§7Name:", textX, textY, 0xAAAAAA);
            String name = truncateText(client, serverInfo.name, panelWidth - 80);
            context.drawTextWithShadow(client.textRenderer, "§f" + name, valueX, textY, 0xFFFFFF);
            textY += lineHeight;
            
            // Server address row
            context.drawTextWithShadow(client.textRenderer, "§7Address:", textX, textY, 0xAAAAAA);
            String addr = truncateText(client, serverInfo.address, panelWidth - 80);
            context.drawTextWithShadow(client.textRenderer, "§b" + addr, valueX, textY, 0x55FFFF);
            textY += lineHeight + sectionGap;
        }
        
        // Connection section with separator
        drawSectionSeparator(context, x, textY - 2, panelWidth, "Connection");
        textY += 10;
        
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler != null) {
            Collection<PlayerListEntry> players = networkHandler.getPlayerList();
            
            // Players online
            context.drawTextWithShadow(client.textRenderer, "§7Players:", textX, textY, 0xAAAAAA);
            context.drawTextWithShadow(client.textRenderer, "§a" + players.size() + " online", valueX, textY, 0x55FF55);
            textY += lineHeight;
            
            // Server software
            String brand = networkHandler.getBrand();
            if (brand != null) {
                context.drawTextWithShadow(client.textRenderer, "§7Software:", textX, textY, 0xAAAAAA);
                String brandText = truncateText(client, brand, panelWidth - 80);
                context.drawTextWithShadow(client.textRenderer, "§d" + brandText, valueX, textY, 0xFF55FF);
                textY += lineHeight;
            }
            
            // Ping
            if (client.player != null) {
                PlayerListEntry entry = networkHandler.getPlayerListEntry(client.player.getUuid());
                if (entry != null) {
                    int ping = entry.getLatency();
                    context.drawTextWithShadow(client.textRenderer, "§7Ping:", textX, textY, 0xAAAAAA);
                    String pingColor = ping < 50 ? "§a" : ping < 150 ? "§e" : "§c";
                    context.drawTextWithShadow(client.textRenderer, pingColor + ping + "ms", valueX, textY, 0xFFFFFF);
                    textY += lineHeight;
                }
            }
        }
        
        // World section
        if (client.player != null && client.world != null) {
            textY += sectionGap - 2;
            drawSectionSeparator(context, x, textY - 2, panelWidth, "World");
            textY += 10;
            
            // Position
            context.drawTextWithShadow(client.textRenderer, "§7Position:", textX, textY, 0xAAAAAA);
            String pos = String.format("§f%.0f §7/ §f%.0f §7/ §f%.0f", 
                client.player.getX(), client.player.getY(), client.player.getZ());
            context.drawTextWithShadow(client.textRenderer, pos, valueX, textY, 0xFFFFFF);
            textY += lineHeight;
            
            // Dimension
            String dim = client.world.getRegistryKey().getValue().getPath();
            context.drawTextWithShadow(client.textRenderer, "§7Dimension:", textX, textY, 0xAAAAAA);
            String dimColor = dim.contains("nether") ? "§c" : dim.contains("end") ? "§5" : "§2";
            context.drawTextWithShadow(client.textRenderer, dimColor + formatDimension(dim), valueX, textY, 0xFFFFFF);
            textY += lineHeight;
            
            // Difficulty
            context.drawTextWithShadow(client.textRenderer, "§7Difficulty:", textX, textY, 0xAAAAAA);
            String diff = client.world.getDifficulty().getName();
            context.drawTextWithShadow(client.textRenderer, "§f" + capitalize(diff), valueX, textY, 0xFFFFFF);
        }
        
        // Close hint at bottom
        context.drawCenteredTextWithShadow(client.textRenderer, "§8Click anywhere to close", 
            x + panelWidth / 2, y + panelHeight - 12, 0x666666);
    }
    
    private int calculatePanelHeight(MinecraftClient client, int lineHeight, int sectionGap) {
        int height = 24; // Title
        height += lineHeight * 2 + sectionGap; // Server section
        height += 10 + lineHeight * 3; // Connection section
        if (client.player != null && client.world != null) {
            height += sectionGap + 10 + lineHeight * 3; // World section
        }
        height += 16; // Close hint
        return height;
    }
    
    private void drawSectionSeparator(DrawContext context, int x, int y, int width, String label) {
        MinecraftClient client = MinecraftClient.getInstance();
        int labelWidth = client.textRenderer.getWidth(label) + 8;
        int lineY = y + 4;
        
        // Left line
        context.fill(x + 8, lineY, x + 8 + 20, lineY + 1, 0xFF404040);
        // Label
        context.drawTextWithShadow(client.textRenderer, "§6" + label, x + 32, y, 0xFFAA00);
        // Right line
        context.fill(x + 32 + labelWidth, lineY, x + width - 8, lineY + 1, 0xFF404040);
    }
    
    private String truncateText(MinecraftClient client, String text, int maxWidth) {
        if (client.textRenderer.getWidth(text) <= maxWidth) return text;
        while (client.textRenderer.getWidth(text + "...") > maxWidth && text.length() > 1) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
    }
    
    private String formatDimension(String dim) {
        return switch (dim) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "The Nether";
            case "the_end" -> "The End";
            default -> capitalize(dim.replace("_", " "));
        };
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
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
