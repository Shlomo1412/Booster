package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.gui.screen.ChatScreen;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Module that adds a Clear Chatbox button to the chat screen.
 * Clears the text currently typed in the chat input field.
 */
public class ClearChatboxModule extends GUIModule {
    
    public static final String CLEAR_CHATBOX_WIDGET_ID = "clear_chatbox";
    
    private BoosterButton clearChatboxButton;
    private Runnable clearAction;
    
    public ClearChatboxModule() {
        super(
            "clear_chatbox",
            "Clear Chatbox",
            "Adds a button to clear the text you've typed in the chat input field.",
            true,
            20,  // Default button width
            20   // Default button height
        );
    }
    
    /**
     * Creates the clear chatbox button for the chat screen.
     *
     * @param screen The chat screen
     * @param anchorX The anchor X position
     * @param anchorY The anchor Y position
     * @param clearAction Action to clear the chat input
     * @param addDrawableChild Callback to add the button
     */
    public void createButton(ChatScreen screen, int anchorX, int anchorY, 
                             Runnable clearAction, Consumer<BoosterButton> addDrawableChild) {
        this.clearAction = clearAction;
        
        // Get per-widget settings
        WidgetSettings settings = getWidgetSettings(CLEAR_CHATBOX_WIDGET_ID, 4, 0);
        
        // Calculate button position
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        clearChatboxButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🧹",  // Broom icon for clearing
            "Clear Input",
            "Clears the text in the chat input field.",
            button -> performClear()
        );
        
        // Skip keyboard navigation to avoid interfering with chat input
        clearChatboxButton.setSkipKeyboardNavigation(true);
        
        // Apply display mode from settings
        clearChatboxButton.setDisplayMode(settings.getDisplayMode());
        
        // Set editor info
        clearChatboxButton.setEditorInfo(this, CLEAR_CHATBOX_WIDGET_ID, "Clear Input", anchorX, anchorY);
        
        addDrawableChild.accept(clearChatboxButton);
    }
    
    /**
     * Gets the buttons created by this module.
     */
    public List<BoosterButton> getButtons() {
        List<BoosterButton> buttons = new ArrayList<>();
        if (clearChatboxButton != null) buttons.add(clearChatboxButton);
        return buttons;
    }
    
    /**
     * Performs the clear action.
     */
    private void performClear() {
        if (clearAction != null) {
            clearAction.run();
        }
    }
    
    @Override
    protected void onEnable() {
        // Nothing special to do on enable
    }
    
    @Override
    protected void onDisable() {
        clearChatboxButton = null;
        clearAction = null;
    }
}
