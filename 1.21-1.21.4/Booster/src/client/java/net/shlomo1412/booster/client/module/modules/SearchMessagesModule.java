package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterSearchField;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Module that adds a search bar to the chat screen.
 * Allows searching through chat message history with highlighting.
 */
public class SearchMessagesModule extends GUIModule {
    
    public static final String SEARCH_MESSAGES_WIDGET_ID = "search_messages";
    
    // Settings
    private final ModuleSetting.BooleanSetting caseSensitiveSetting;
    private final ModuleSetting.ColorSetting highlightColorSetting;
    
    private BoosterSearchField searchField;
    private String currentSearchQuery = "";
    private List<Integer> matchingLineIndices = new ArrayList<>();
    private int currentMatchIndex = -1;
    
    public SearchMessagesModule() {
        super(
            "search_messages",
            "Search Messages",
            "Adds a search bar to find messages in your chat history.\n" +
            "Press Enter to cycle through matches.\n" +
            "Matching messages will be highlighted.",
            true,
            100, // Default width
            14   // Default height
        );
        
        // Case sensitive setting
        this.caseSensitiveSetting = new ModuleSetting.BooleanSetting(
            "case_sensitive",
            "Case Sensitive",
            "Whether the search should be case-sensitive",
            false
        );
        registerSetting(caseSensitiveSetting);
        
        // Highlight color setting
        this.highlightColorSetting = new ModuleSetting.ColorSetting(
            "highlight_color",
            "Highlight Color",
            "Color used to highlight matching messages",
            0xFFFFAA00  // Orange
        );
        registerSetting(highlightColorSetting);
    }
    
    /**
     * Creates the search field for the chat screen.
     *
     * @param screen The chat screen
     * @param anchorX The anchor X position
     * @param anchorY The anchor Y position
     * @param screenWidth The screen width for positioning
     * @param addDrawableChild Callback to add the widget
     */
    public void createSearchField(ChatScreen screen, int anchorX, int anchorY, int screenWidth,
                                  Consumer<BoosterSearchField> addDrawableChild) {
        // Get per-widget settings - position at top right of screen
        WidgetSettings settings = getWidgetSettings(SEARCH_MESSAGES_WIDGET_ID, screenWidth - 104, -anchorY + 4);
        
        // Calculate field position
        int fieldX = anchorX + settings.getOffsetX();
        int fieldY = anchorY + settings.getOffsetY();
        
        searchField = new BoosterSearchField(
            fieldX, fieldY,
            settings.getWidth(), settings.getHeight(),
            Text.literal("Search chat...")
        );
        
        // Skip keyboard navigation to avoid interfering with chat input
        searchField.setSkipKeyboardNavigation(true);
        
        // Set up text change listener
        searchField.setChangedListener(this::onSearchTextChanged);
        
        // Set editor info
        searchField.setEditorInfo(this, SEARCH_MESSAGES_WIDGET_ID, "Search Messages", anchorX, anchorY);
        
        // Reset search state
        currentSearchQuery = "";
        matchingLineIndices.clear();
        currentMatchIndex = -1;
        
        addDrawableChild.accept(searchField);
    }
    
    /**
     * Gets the search field.
     */
    public BoosterSearchField getSearchField() {
        return searchField;
    }
    
    /**
     * Called when the search text changes.
     */
    private void onSearchTextChanged(String text) {
        currentSearchQuery = text;
        updateMatchingMessages();
    }
    
    /**
     * Updates the list of matching messages.
     */
    private void updateMatchingMessages() {
        matchingLineIndices.clear();
        currentMatchIndex = -1;
        
        if (currentSearchQuery.isEmpty()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return;
        
        ChatHud chatHud = client.inGameHud.getChatHud();
        List<ChatHudLine> messages = getChatMessages(chatHud);
        
        if (messages == null) return;
        
        String query = caseSensitiveSetting.getValue() ? currentSearchQuery : currentSearchQuery.toLowerCase();
        
        for (int i = 0; i < messages.size(); i++) {
            ChatHudLine line = messages.get(i);
            String messageText = line.content().getString();
            if (!caseSensitiveSetting.getValue()) {
                messageText = messageText.toLowerCase();
            }
            
            if (messageText.contains(query)) {
                matchingLineIndices.add(i);
            }
        }
        
        if (!matchingLineIndices.isEmpty()) {
            currentMatchIndex = 0;
        }
    }
    
    /**
     * Cycles to the next matching message.
     */
    public void cycleNextMatch() {
        if (matchingLineIndices.isEmpty()) return;
        
        currentMatchIndex = (currentMatchIndex + 1) % matchingLineIndices.size();
        scrollToCurrentMatch();
    }
    
    /**
     * Cycles to the previous matching message.
     */
    public void cyclePreviousMatch() {
        if (matchingLineIndices.isEmpty()) return;
        
        currentMatchIndex = (currentMatchIndex - 1 + matchingLineIndices.size()) % matchingLineIndices.size();
        scrollToCurrentMatch();
    }
    
    /**
     * Scrolls to the currently selected match.
     */
    private void scrollToCurrentMatch() {
        if (currentMatchIndex < 0 || currentMatchIndex >= matchingLineIndices.size()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return;
        
        ChatHud chatHud = client.inGameHud.getChatHud();
        int messageIndex = matchingLineIndices.get(currentMatchIndex);
        
        // Scroll to make the message visible
        // The chat scrolls based on lines, so we need to calculate the scroll position
        chatHud.scroll(messageIndex);
    }
    
    /**
     * Gets the current search query.
     */
    public String getCurrentSearchQuery() {
        return currentSearchQuery;
    }
    
    /**
     * Gets the list of matching line indices.
     */
    public List<Integer> getMatchingLineIndices() {
        return matchingLineIndices;
    }
    
    /**
     * Gets the current match index.
     */
    public int getCurrentMatchIndex() {
        return currentMatchIndex;
    }
    
    /**
     * Gets the highlight color.
     */
    public int getHighlightColor() {
        return highlightColorSetting.getValue();
    }
    
    /**
     * Checks if a message at the given index matches the search.
     */
    public boolean isMessageMatching(int index) {
        return matchingLineIndices.contains(index);
    }
    
    /**
     * Checks if a message at the given index is the current match.
     */
    public boolean isCurrentMatch(int index) {
        return currentMatchIndex >= 0 && 
               currentMatchIndex < matchingLineIndices.size() && 
               matchingLineIndices.get(currentMatchIndex) == index;
    }
    
    /**
     * Renders the search status (e.g., "3/10 matches").
     */
    public void renderSearchStatus(DrawContext context, int x, int y) {
        if (searchField == null || currentSearchQuery.isEmpty()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        String status;
        
        if (matchingLineIndices.isEmpty()) {
            status = "No matches";
        } else {
            status = String.format("%d/%d", currentMatchIndex + 1, matchingLineIndices.size());
        }
        
        // Draw status next to search field
        int textX = searchField.getX() + searchField.getWidth() + 4;
        int textY = searchField.getY() + (searchField.getHeight() - 8) / 2;
        
        context.drawTextWithShadow(client.textRenderer, status, textX, textY, 0xAAAAAA);
    }
    
    /**
     * Accesses the chat messages using reflection or accessor.
     * This may need to be adapted based on Minecraft version.
     */
    @SuppressWarnings("unchecked")
    private List<ChatHudLine> getChatMessages(ChatHud chatHud) {
        try {
            // Try to access the messages field via reflection
            // In 1.21.x, the field is typically named "messages" or similar
            java.lang.reflect.Field messagesField = null;
            for (java.lang.reflect.Field field : ChatHud.class.getDeclaredFields()) {
                if (java.util.List.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(chatHud);
                    if (value instanceof List<?> list && !list.isEmpty()) {
                        Object first = list.get(0);
                        if (first instanceof ChatHudLine) {
                            return (List<ChatHudLine>) value;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Reflection failed, return empty list
        }
        return new ArrayList<>();
    }
    
    /**
     * Handles key press events for the search field.
     * Returns true if the event was handled.
     */
    public boolean handleKeyPress(int keyCode) {
        if (searchField == null || !searchField.isFocused()) return false;
        
        // Enter key - cycle to next match
        if (keyCode == 257) { // ENTER
            cycleNextMatch();
            return true;
        }
        
        // Shift+Enter - cycle to previous match
        if (keyCode == 257 && net.minecraft.client.gui.screen.Screen.hasShiftDown()) {
            cyclePreviousMatch();
            return true;
        }
        
        return false;
    }
    
    @Override
    protected void onEnable() {
        // Nothing special to do on enable
    }
    
    @Override
    protected void onDisable() {
        searchField = null;
        currentSearchQuery = "";
        matchingLineIndices.clear();
        currentMatchIndex = -1;
    }
}
