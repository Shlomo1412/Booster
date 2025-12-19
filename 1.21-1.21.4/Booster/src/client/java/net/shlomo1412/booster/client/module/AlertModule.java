package net.shlomo1412.booster.client.module;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

/**
 * Base class for alert modules that notify the player of various conditions.
 * Provides common settings for message type, color, formatting, sound, and cooldown.
 */
public abstract class AlertModule extends Module {
    
    // Message display types
    public enum MessageType {
        CHAT("Chat Message"),
        ACTION_BAR("Action Bar"),
        TITLE("Title"),
        SUBTITLE("Subtitle"),
        TITLE_AND_SUBTITLE("Title + Subtitle");
        
        private final String displayName;
        
        MessageType(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // Text formatting options
    public enum TextFormat {
        NONE("None"),
        BOLD("Bold"),
        ITALIC("Italic"),
        UNDERLINE("Underlined"),
        STRIKETHROUGH("Strikethrough"),
        OBFUSCATED("Obfuscated"),
        BOLD_ITALIC("Bold + Italic");
        
        private final String displayName;
        
        TextFormat(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // Common settings for all alert modules
    protected final ModuleSetting.EnumSetting<MessageType> messageTypeSetting;
    protected final ModuleSetting.ColorSetting colorSetting;
    protected final ModuleSetting.EnumSetting<TextFormat> formatSetting;
    protected final ModuleSetting.BooleanSetting playSoundSetting;
    protected final ModuleSetting.NumberSetting cooldownSetting;
    protected final ModuleSetting.BooleanSetting enabledSetting;
    
    // Runtime state
    protected long lastAlertTime = 0;
    private static boolean tickRegistered = false;
    
    public AlertModule(String id, String name, String description, boolean defaultEnabled,
                       int defaultColor, MessageType defaultMessageType, int defaultCooldownSeconds) {
        super(id, name, description, defaultEnabled);
        
        // Alert enabled toggle
        this.enabledSetting = new ModuleSetting.BooleanSetting(
            "alert_enabled",
            "Alert Enabled",
            "Enable or disable this alert",
            true
        );
        registerSetting(enabledSetting);
        
        // Message type setting
        this.messageTypeSetting = new ModuleSetting.EnumSetting<>(
            "message_type",
            "Display Type",
            "How the alert message is displayed",
            defaultMessageType,
            MessageType.class
        );
        registerSetting(messageTypeSetting);
        
        // Color setting
        this.colorSetting = new ModuleSetting.ColorSetting(
            "color",
            "Text Color",
            "Color of the alert message",
            defaultColor
        );
        registerSetting(colorSetting);
        
        // Format setting
        this.formatSetting = new ModuleSetting.EnumSetting<>(
            "format",
            "Text Format",
            "Text formatting style",
            TextFormat.NONE,
            TextFormat.class
        );
        registerSetting(formatSetting);
        
        // Sound setting
        this.playSoundSetting = new ModuleSetting.BooleanSetting(
            "play_sound",
            "Play Sound",
            "Play a sound when the alert triggers",
            true
        );
        registerSetting(playSoundSetting);
        
        // Cooldown setting (in seconds)
        this.cooldownSetting = new ModuleSetting.NumberSetting(
            "cooldown",
            "Cooldown (seconds)",
            "Minimum time between alerts",
            defaultCooldownSeconds,
            1,
            60
        );
        registerSetting(cooldownSetting);
    }
    
    @Override
    protected void onEnable() {
        super.onEnable();
        registerTickHandler();
    }
    
    /**
     * Registers the client tick handler for all alert modules.
     * Only registers once, then each alert module checks conditions.
     */
    public static void registerTickHandler() {
        if (!tickRegistered) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client.player != null && client.world != null) {
                    ModuleManager.getInstance().getModules().stream()
                        .filter(m -> m instanceof AlertModule)
                        .map(m -> (AlertModule) m)
                        .filter(Module::isEnabled)
                        .filter(a -> a.enabledSetting.getValue())
                        .forEach(a -> a.checkAndAlert(client));
                }
            });
            tickRegistered = true;
        }
    }
    
    /**
     * Override this to check conditions and trigger alerts.
     * Called every tick when the module is enabled.
     */
    protected abstract void checkAndAlert(MinecraftClient client);
    
    /**
     * Checks if enough time has passed since the last alert.
     */
    protected boolean canAlert() {
        long now = System.currentTimeMillis();
        long cooldownMs = cooldownSetting.getValue() * 1000L;
        return now - lastAlertTime >= cooldownMs;
    }
    
    /**
     * Sends an alert to the player with the configured settings.
     */
    protected void sendAlert(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        lastAlertTime = System.currentTimeMillis();
        
        // Create styled text
        Text styledText = createStyledText(message);
        
        // Display based on message type
        MessageType type = messageTypeSetting.getValue();
        switch (type) {
            case CHAT -> player.sendMessage(styledText, false);
            case ACTION_BAR -> player.sendMessage(styledText, true);
            case TITLE -> client.inGameHud.setTitle(styledText);
            case SUBTITLE -> client.inGameHud.setSubtitle(styledText);
            case TITLE_AND_SUBTITLE -> {
                client.inGameHud.setTitle(styledText);
                client.inGameHud.setSubtitle(Text.literal(""));
            }
        }
        
        // Set title times if showing title/subtitle
        if (type == MessageType.TITLE || type == MessageType.SUBTITLE || type == MessageType.TITLE_AND_SUBTITLE) {
            client.inGameHud.setTitleTicks(10, 40, 10); // fade in, stay, fade out
        }
        
        // Play sound if enabled
        if (playSoundSetting.getValue()) {
            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
    
    /**
     * Sends an alert with separate title and subtitle text.
     */
    protected void sendAlert(String title, String subtitle) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        lastAlertTime = System.currentTimeMillis();
        
        Text styledTitle = createStyledText(title);
        Text styledSubtitle = createStyledText(subtitle);
        
        MessageType type = messageTypeSetting.getValue();
        switch (type) {
            case CHAT -> {
                player.sendMessage(styledTitle, false);
                player.sendMessage(styledSubtitle, false);
            }
            case ACTION_BAR -> player.sendMessage(Text.literal(title + " - " + subtitle)
                .setStyle(getStyle()), true);
            case TITLE -> client.inGameHud.setTitle(styledTitle);
            case SUBTITLE -> client.inGameHud.setSubtitle(styledSubtitle);
            case TITLE_AND_SUBTITLE -> {
                client.inGameHud.setTitle(styledTitle);
                client.inGameHud.setSubtitle(styledSubtitle);
            }
        }
        
        if (type == MessageType.TITLE || type == MessageType.SUBTITLE || type == MessageType.TITLE_AND_SUBTITLE) {
            client.inGameHud.setTitleTicks(10, 40, 10);
        }
        
        if (playSoundSetting.getValue()) {
            player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
    
    /**
     * Creates styled text based on current settings.
     */
    protected Text createStyledText(String message) {
        MutableText text = Text.literal(message);
        text.setStyle(getStyle());
        return text;
    }
    
    /**
     * Gets the style based on current settings.
     */
    protected Style getStyle() {
        int color = colorSetting.getValue();
        TextFormat format = formatSetting.getValue();
        
        Style style = Style.EMPTY.withColor(color);
        
        switch (format) {
            case BOLD -> style = style.withBold(true);
            case ITALIC -> style = style.withItalic(true);
            case UNDERLINE -> style = style.withUnderline(true);
            case STRIKETHROUGH -> style = style.withStrikethrough(true);
            case OBFUSCATED -> style = style.withObfuscated(true);
            case BOLD_ITALIC -> style = style.withBold(true).withItalic(true);
            case NONE -> {} // No formatting
        }
        
        return style;
    }
    
    /**
     * Registers a setting specific to this alert module.
     * This is a helper to expose the protected registerSetting method.
     */
    protected void registerAlertSetting(ModuleSetting<?> setting) {
        registerSetting(setting);
    }
    
    /**
     * Helper method for registering settings (needed since GUIModule.registerSetting is protected)
     */
    protected void registerSetting(ModuleSetting<?> setting) {
        // We need to store settings in a map - for now we'll use inheritance from GUIModule
        // But since we extend Module, we need our own storage
        alertSettings.put(setting.getId(), setting);
    }
    
    // Storage for alert-specific settings
    private final java.util.Map<String, ModuleSetting<?>> alertSettings = new java.util.LinkedHashMap<>();
    
    /**
     * Gets a setting by ID.
     */
    @SuppressWarnings("unchecked")
    public <T> ModuleSetting<T> getSetting(String id) {
        return (ModuleSetting<T>) alertSettings.get(id);
    }
    
    /**
     * Gets all settings.
     */
    public java.util.Collection<ModuleSetting<?>> getSettings() {
        return alertSettings.values();
    }
    
    /**
     * Gets all settings as a map.
     */
    public java.util.Map<String, ModuleSetting<?>> getAllSettings() {
        return alertSettings;
    }
    
    /**
     * Checks if this module has settings.
     */
    public boolean hasSettings() {
        return !alertSettings.isEmpty();
    }
    
    /**
     * Updates a setting value and saves config.
     */
    @SuppressWarnings("unchecked")
    public <T> void updateSetting(String id, T value) {
        ModuleSetting<T> setting = (ModuleSetting<T>) alertSettings.get(id);
        if (setting != null) {
            setting.setValue(value);
            ModuleManager.getInstance().saveConfig();
        }
    }
    
    /**
     * Resets all settings to defaults.
     */
    public void resetAllSettings() {
        for (ModuleSetting<?> setting : alertSettings.values()) {
            setting.reset();
        }
        ModuleManager.getInstance().saveConfig();
    }
}
