package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.shlomo1412.booster.client.module.AlertModule;
import net.shlomo1412.booster.client.module.ModuleSetting;

import java.util.regex.Pattern;

/**
 * Module that alerts the player when their name is mentioned in chat.
 * Fully customizable with message type, color, format, sound, and cooldown.
 * This module is triggered by a mixin that intercepts chat messages.
 */
public class MentionedInChatAlertModule extends AlertModule {
    
    private final ModuleSetting.BooleanSetting caseSensitiveSetting;
    private final ModuleSetting.BooleanSetting highlightMessageSetting;
    private final ModuleSetting.BooleanSetting customKeywordsSetting;
    private Pattern mentionPattern = null;
    
    // Custom keywords to also trigger mention alerts (comma-separated in config)
    private String additionalKeywords = "";
    
    public MentionedInChatAlertModule() {
        super(
            "mention_alert",
            "Mentioned in Chat Alert",
            "Alerts when your name is mentioned in chat.\n" +
            "Also supports custom keywords to watch for.\n" +
            "Fully customizable: display type, color, format, sound.",
            true,
            0xFF55FF55,  // Bright green
            MessageType.TITLE_AND_SUBTITLE,
            1  // 1 second cooldown
        );
        
        // Case sensitive setting
        this.caseSensitiveSetting = new ModuleSetting.BooleanSetting(
            "case_sensitive",
            "Case Sensitive",
            "Whether to match the exact case of your name",
            false
        );
        registerSetting(caseSensitiveSetting);
        
        // Highlight message setting
        this.highlightMessageSetting = new ModuleSetting.BooleanSetting(
            "highlight_message",
            "Highlight Message",
            "Also highlight the chat message that mentioned you",
            true
        );
        registerSetting(highlightMessageSetting);
        
        // Custom keywords setting (placeholder - would need string setting)
        this.customKeywordsSetting = new ModuleSetting.BooleanSetting(
            "use_custom_keywords",
            "Use Custom Keywords",
            "Also alert for custom keywords (configured in booster.json)",
            false
        );
        registerSetting(customKeywordsSetting);
    }
    
    @Override
    protected void checkAndAlert(MinecraftClient client) {
        // This module doesn't check on tick - it's triggered by the chat mixin
        // Nothing to do here
    }
    
    /**
     * Called by the chat mixin when a message is received.
     * Returns true if the message contains a mention.
     */
    public boolean checkForMention(Text message, String playerName) {
        if (!isEnabled() || !enabledSetting.getValue()) return false;
        
        String messageText = message.getString();
        
        // Build mention pattern
        Pattern pattern = getMentionPattern(playerName);
        
        if (pattern.matcher(messageText).find()) {
            if (canAlert()) {
                // Extract the sender if possible (usually before the colon)
                String sender = extractSender(messageText);
                
                if (sender != null && !sender.equals(playerName)) {
                    sendAlert("📢 Mentioned!", sender + " mentioned you in chat");
                } else if (!messageText.startsWith("<" + playerName + ">")) {
                    // Only alert if it's not our own message
                    sendAlert("📢 You were mentioned in chat!");
                } else {
                    return false; // Don't alert for our own messages
                }
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gets or builds the mention pattern for the player name.
     */
    private Pattern getMentionPattern(String playerName) {
        // Rebuild pattern if needed
        StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append("\\b").append(Pattern.quote(playerName)).append("\\b");
        
        if (customKeywordsSetting.getValue() && !additionalKeywords.isEmpty()) {
            String[] keywords = additionalKeywords.split(",");
            for (String keyword : keywords) {
                keyword = keyword.trim();
                if (!keyword.isEmpty()) {
                    patternBuilder.append("|\\b").append(Pattern.quote(keyword)).append("\\b");
                }
            }
        }
        
        int flags = caseSensitiveSetting.getValue() ? 0 : Pattern.CASE_INSENSITIVE;
        return Pattern.compile(patternBuilder.toString(), flags);
    }
    
    /**
     * Tries to extract the sender name from a chat message.
     * Common formats: <Player> message, [Player] message, Player: message
     */
    private String extractSender(String message) {
        // Try <Player> format
        if (message.startsWith("<")) {
            int end = message.indexOf('>');
            if (end > 1) {
                return message.substring(1, end);
            }
        }
        
        // Try [Player] format
        if (message.startsWith("[")) {
            int end = message.indexOf(']');
            if (end > 1) {
                String content = message.substring(1, end);
                // Could be a server prefix or player name
                if (!content.contains(" ")) {
                    return content;
                }
            }
        }
        
        // Try Player: format
        int colonIndex = message.indexOf(':');
        if (colonIndex > 0 && colonIndex < 20) {
            String potential = message.substring(0, colonIndex).trim();
            // Check if it looks like a player name (no spaces, reasonable length)
            if (!potential.contains(" ") && potential.length() >= 3 && potential.length() <= 16) {
                return potential;
            }
        }
        
        return null;
    }
    
    /**
     * Sets additional keywords to watch for (comma-separated).
     * This would typically be loaded from config.
     */
    public void setAdditionalKeywords(String keywords) {
        this.additionalKeywords = keywords != null ? keywords : "";
        this.mentionPattern = null; // Force rebuild
    }
    
    /**
     * Gets whether to highlight the chat message.
     */
    public boolean shouldHighlightMessage() {
        return highlightMessageSetting.getValue();
    }
    
    @Override
    protected void onDisable() {
        mentionPattern = null;
    }
}
