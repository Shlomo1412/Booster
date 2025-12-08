package net.shlomo1412.booster.client.module;

/**
 * Base class for GUI-related modules that add widgets to screens.
 * Supports relative coordinates for positioning elements.
 */
public abstract class GUIModule extends Module {
    private int offsetX;
    private int offsetY;
    private final int defaultOffsetX;
    private final int defaultOffsetY;

    /**
     * Creates a new GUI module.
     *
     * @param id              Unique identifier for this module
     * @param name            Display name of the module
     * @param description     Description of what this module does
     * @param defaultEnabled  Whether this module is enabled by default
     * @param defaultOffsetX  Default X offset from the anchor point
     * @param defaultOffsetY  Default Y offset from the anchor point
     */
    public GUIModule(String id, String name, String description, boolean defaultEnabled, 
                     int defaultOffsetX, int defaultOffsetY) {
        super(id, name, description, defaultEnabled);
        this.defaultOffsetX = defaultOffsetX;
        this.defaultOffsetY = defaultOffsetY;
        this.offsetX = defaultOffsetX;
        this.offsetY = defaultOffsetY;
    }

    /**
     * @return The X offset from the anchor point
     */
    public int getOffsetX() {
        return offsetX;
    }

    /**
     * @return The Y offset from the anchor point
     */
    public int getOffsetY() {
        return offsetY;
    }

    /**
     * Sets the X offset.
     *
     * @param offsetX The new X offset
     */
    public void setOffsetX(int offsetX) {
        if (this.offsetX != offsetX) {
            this.offsetX = offsetX;
            ModuleManager.getInstance().saveConfig();
        }
    }

    /**
     * Sets the Y offset.
     *
     * @param offsetY The new Y offset
     */
    public void setOffsetY(int offsetY) {
        if (this.offsetY != offsetY) {
            this.offsetY = offsetY;
            ModuleManager.getInstance().saveConfig();
        }
    }

    /**
     * Sets both offsets at once.
     *
     * @param offsetX The new X offset
     * @param offsetY The new Y offset
     */
    public void setOffset(int offsetX, int offsetY) {
        boolean changed = this.offsetX != offsetX || this.offsetY != offsetY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        if (changed) {
            ModuleManager.getInstance().saveConfig();
        }
    }

    /**
     * Resets offsets to their default values.
     */
    public void resetOffset() {
        setOffset(defaultOffsetX, defaultOffsetY);
    }

    /**
     * @return The default X offset
     */
    public int getDefaultOffsetX() {
        return defaultOffsetX;
    }

    /**
     * @return The default Y offset
     */
    public int getDefaultOffsetY() {
        return defaultOffsetY;
    }
}
