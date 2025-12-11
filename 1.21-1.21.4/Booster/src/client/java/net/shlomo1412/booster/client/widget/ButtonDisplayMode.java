package net.shlomo1412.booster.client.widget;

/**
 * Display mode for buttons - determines how the button content is shown.
 */
public enum ButtonDisplayMode {
    ICON_ONLY("Icon Only", "Shows only the icon"),
    NAME_AND_ICON("Name + Icon", "Shows both the name and icon"),
    AUTO("Automatic", "Automatically adjusts based on button size");
    
    private final String displayName;
    private final String description;
    
    ButtonDisplayMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * @return The next mode in the cycle
     */
    public ButtonDisplayMode next() {
        ButtonDisplayMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
    
    /**
     * Determines the minimum width needed to show both icon and name.
     * @param textWidth The width of the name text in pixels
     * @return The minimum button width needed
     */
    public static int getMinWidthForNameAndIcon(int textWidth) {
        return textWidth + 24; // Icon (12px) + padding (12px) + text width
    }
}
