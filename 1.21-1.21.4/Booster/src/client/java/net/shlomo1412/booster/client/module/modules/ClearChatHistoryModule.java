package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Module that adds a Clear Chat History button to the chat screen.
 * Clears all messages from the chat log.
 */
public class ClearChatHistoryModule extends GUIModule {
    
    public static final String CLEAR_HISTORY_WIDGET_ID = "clear_chat_history";
    
    // Settings
    private final ModuleSetting.BooleanSetting confirmClearSetting;
    
    private BoosterButton clearHistoryButton;
    
    public ClearChatHistoryModule() {
        super(
            "clear_chat_history",
            "Clear Chat History",
            "Adds a button to clear all messages from your chat history.\n" +
            "Hold SHIFT to bypass confirmation.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Confirm clear setting
        this.confirmClearSetting = new ModuleSetting.BooleanSetting(
            "confirm_clear",
            "Require SHIFT",
            "Require holding SHIFT to clear history (prevents accidents)",
            true
        );
        registerSetting(confirmClearSetting);
    }
    
    /**
     * Creates the clear chat history button for the chat screen.
     *
     * @param screen The chat screen
     * @param anchorX The anchor X position
     * @param anchorY The anchor Y position
     * @param addDrawableChild Callback to add the button
     */
    public void createButton(ChatScreen screen, int anchorX, int anchorY, 
                             Consumer<BoosterButton> addDrawableChild) {
        // Get per-widget settings
        WidgetSettings settings = getWidgetSettings(CLEAR_HISTORY_WIDGET_ID, 26, 0);
        
        // Calculate button position
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        String tooltip = confirmClearSetting.getValue()
            ? "Clears all chat messages.\nHold SHIFT and click to clear."
            : "Clears all chat messages.";
        
        clearHistoryButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🗑",  // Trash icon
            "Clear History",
            tooltip,
            button -> clearChatHistory()
        );
        
        // Skip keyboard navigation to avoid interfering with chat input
        clearHistoryButton.setSkipKeyboardNavigation(true);
        
        // Apply display mode from settings
        clearHistoryButton.setDisplayMode(settings.getDisplayMode());
        
        // Set editor info
        clearHistoryButton.setEditorInfo(this, CLEAR_HISTORY_WIDGET_ID, "Clear History", anchorX, anchorY);
        
        addDrawableChild.accept(clearHistoryButton);
    }
    
    /**
     * Gets the buttons created by this module.
     */
    public List<BoosterButton> getButtons() {
        List<BoosterButton> buttons = new ArrayList<>();
        if (clearHistoryButton != null) buttons.add(clearHistoryButton);
        return buttons;
    }
    
    /**
     * Clears the chat history.
     */
    private void clearChatHistory() {
        // Check if SHIFT is required and held
        if (confirmClearSetting.getValue() && !net.minecraft.client.gui.screen.Screen.hasShiftDown()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            // In 1.21.x, use clear() with deleteHistory parameter
            // true = also delete the command history, false = only clear visible messages
            client.inGameHud.getChatHud().clear(true);
        }
    }
    
    @Override
    protected void onEnable() {
        // Nothing special to do on enable
    }
    
    @Override
    protected void onDisable() {
        clearHistoryButton = null;
    }
}
