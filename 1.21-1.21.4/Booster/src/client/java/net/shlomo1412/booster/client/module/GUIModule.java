package net.shlomo1412.booster.client.module;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Base class for GUI-related modules that add widgets to screens.
 * Supports multiple widgets with individual position/size settings.
 */
public abstract class GUIModule extends Module {
    
    // Per-widget settings stored by widget ID
    private final Map<String, WidgetSettings> widgetSettings = new HashMap<>();
    
    // Loaded values from config (before defaults are known)
    private final Map<String, int[]> loadedWidgetValues = new HashMap<>();
    
    // Default settings for new widgets
    private final int defaultWidth;
    private final int defaultHeight;

    /**
     * Creates a new GUI module with default 20x20 widget size.
     */
    public GUIModule(String id, String name, String description, boolean defaultEnabled) {
        this(id, name, description, defaultEnabled, 20, 20);
    }
    
    /**
     * Creates a new GUI module with custom default widget size.
     */
    public GUIModule(String id, String name, String description, boolean defaultEnabled,
                     int defaultWidth, int defaultHeight) {
        super(id, name, description, defaultEnabled);
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
    }
    
    /**
     * Gets or creates settings for a specific widget.
     * If values were loaded from config, they will be applied.
     * 
     * @param widgetId Unique identifier for the widget within this module
     * @param defaultOffsetX Default X offset for this widget
     * @param defaultOffsetY Default Y offset for this widget
     * @return The widget settings
     */
    public WidgetSettings getWidgetSettings(String widgetId, int defaultOffsetX, int defaultOffsetY) {
        WidgetSettings settings = widgetSettings.get(widgetId);
        if (settings == null) {
            // Create new settings with proper defaults
            settings = new WidgetSettings(defaultOffsetX, defaultOffsetY, defaultWidth, defaultHeight);
            
            // Apply loaded values if any
            int[] loaded = loadedWidgetValues.remove(widgetId);
            if (loaded != null) {
                settings.setOffset(loaded[0], loaded[1]);
                settings.setSize(loaded[2], loaded[3]);
            }
            
            widgetSettings.put(widgetId, settings);
        }
        return settings;
    }
    
    /**
     * Loads widget settings from config (before defaults are known).
     * Values are stored temporarily until getWidgetSettings is called with defaults.
     */
    public void loadWidgetSettings(String widgetId, int offsetX, int offsetY, int width, int height) {
        loadedWidgetValues.put(widgetId, new int[] { offsetX, offsetY, width, height });
    }
    
    /**
     * Gets settings for a widget if they exist.
     */
    public WidgetSettings getWidgetSettings(String widgetId) {
        return widgetSettings.get(widgetId);
    }
    
    /**
     * Sets settings for a widget (used when loading from config).
     */
    public void setWidgetSettings(String widgetId, WidgetSettings settings) {
        widgetSettings.put(widgetId, settings);
    }
    
    /**
     * Gets all widget IDs that have settings.
     */
    public Set<String> getWidgetIds() {
        return widgetSettings.keySet();
    }
    
    /**
     * Gets all widget settings.
     */
    public Map<String, WidgetSettings> getAllWidgetSettings() {
        return widgetSettings;
    }
    
    /**
     * Updates widget offset and triggers config save.
     */
    public void updateWidgetOffset(String widgetId, int offsetX, int offsetY) {
        WidgetSettings settings = widgetSettings.get(widgetId);
        if (settings != null) {
            settings.setOffset(offsetX, offsetY);
            ModuleManager.getInstance().saveConfig();
        }
    }
    
    /**
     * Updates widget size and triggers config save.
     */
    public void updateWidgetSize(String widgetId, int width, int height) {
        WidgetSettings settings = widgetSettings.get(widgetId);
        if (settings != null) {
            settings.setSize(width, height);
            ModuleManager.getInstance().saveConfig();
        }
    }
    
    /**
     * Resets a widget's position to defaults.
     */
    public void resetWidgetOffset(String widgetId) {
        WidgetSettings settings = widgetSettings.get(widgetId);
        if (settings != null) {
            settings.resetOffset();
            ModuleManager.getInstance().saveConfig();
        }
    }
    
    /**
     * Resets a widget's size to defaults.
     */
    public void resetWidgetSize(String widgetId) {
        WidgetSettings settings = widgetSettings.get(widgetId);
        if (settings != null) {
            settings.resetSize();
            ModuleManager.getInstance().saveConfig();
        }
    }
    
    /**
     * Resets all widgets to default settings.
     */
    public void resetAllWidgets() {
        for (WidgetSettings settings : widgetSettings.values()) {
            settings.reset();
        }
        ModuleManager.getInstance().saveConfig();
    }
    
    /**
     * Resets all widget positions to defaults.
     */
    public void resetAllOffsets() {
        for (WidgetSettings settings : widgetSettings.values()) {
            settings.resetOffset();
        }
        ModuleManager.getInstance().saveConfig();
    }
    
    /**
     * Resets all widget sizes to defaults.
     */
    public void resetAllSizes() {
        for (WidgetSettings settings : widgetSettings.values()) {
            settings.resetSize();
        }
        ModuleManager.getInstance().saveConfig();
    }
    
    public int getDefaultWidth() {
        return defaultWidth;
    }
    
    public int getDefaultHeight() {
        return defaultHeight;
    }
}
