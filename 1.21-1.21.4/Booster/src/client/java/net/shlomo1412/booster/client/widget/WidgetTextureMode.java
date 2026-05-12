package net.shlomo1412.booster.client.widget;

/**
 * Per-widget background texture mode.
 */
public enum WidgetTextureMode {
    DEFAULT("Default"),
    TRANSPARENT("Transparent"),
    INVENTORY("Inventory");

    private final String displayName;

    WidgetTextureMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public WidgetTextureMode next() {
        WidgetTextureMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}