package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerInfo;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds a "Copy IP" button to the server selection screen.
 * Copies the selected server's IP address to the clipboard.
 */
public class CopyIPModule extends GUIModule {
    
    public static final String COPY_IP_WIDGET_ID = "copy_ip";
    
    private BoosterButton copyButton;
    
    public CopyIPModule() {
        super(
            "copy_ip",
            "Copy IP",
            "Adds a button to copy the selected server's IP address to clipboard.",
            true,
            60,  // Default button width (wider for text)
            20   // Default button height
        );
    }
    
    /**
     * Creates the Copy IP button for the multiplayer screen.
     *
     * @param screen The multiplayer screen
     * @param anchorX The anchor X position
     * @param anchorY The anchor Y position
     * @param serverInfoSupplier Supplier to get the currently selected server
     * @param addDrawableChild Callback to add the button
     */
    public void createButton(MultiplayerScreen screen, int anchorX, int anchorY,
                            java.util.function.Supplier<ServerInfo> serverInfoSupplier,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(COPY_IP_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        copyButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "📋",
            "Copy IP",
            "Copies the selected server's IP address to your clipboard.\n" +
            "Select a server from the list first.",
            button -> {
                ServerInfo serverInfo = serverInfoSupplier.get();
                if (serverInfo != null) {
                    String ip = serverInfo.address;
                    MinecraftClient.getInstance().keyboard.setClipboard(ip);
                    // Flash the button or show feedback
                    button.setMessage(net.minecraft.text.Text.literal("✓ Copied!"));
                    // Reset after a short delay would be nice, but simple approach for now
                }
            }
        );
        
        // Apply display mode from settings
        copyButton.setDisplayMode(settings.getDisplayMode());
        
        copyButton.setEditorInfo(this, COPY_IP_WIDGET_ID, "Copy IP", anchorX, anchorY);
        
        addDrawableChild.accept(copyButton);
    }
    
    /**
     * Gets the button created by this module.
     */
    public BoosterButton getButton() {
        return copyButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        copyButton = null;
    }
}
