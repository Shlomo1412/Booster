package net.shlomo1412.booster.client.module;

/**
 * Stores position and size settings for an individual widget.
 * Each widget in a module can have its own settings.
 */
public class WidgetSettings {
    private int offsetX;
    private int offsetY;
    private int width;
    private int height;
    
    // Defaults for resetting
    private final int defaultOffsetX;
    private final int defaultOffsetY;
    private final int defaultWidth;
    private final int defaultHeight;
    
    public WidgetSettings(int defaultOffsetX, int defaultOffsetY, int defaultWidth, int defaultHeight) {
        this.defaultOffsetX = defaultOffsetX;
        this.defaultOffsetY = defaultOffsetY;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
        
        this.offsetX = defaultOffsetX;
        this.offsetY = defaultOffsetY;
        this.width = defaultWidth;
        this.height = defaultHeight;
    }
    
    // Copy constructor for cloning defaults
    public WidgetSettings(WidgetSettings other) {
        this.defaultOffsetX = other.defaultOffsetX;
        this.defaultOffsetY = other.defaultOffsetY;
        this.defaultWidth = other.defaultWidth;
        this.defaultHeight = other.defaultHeight;
        
        this.offsetX = other.offsetX;
        this.offsetY = other.offsetY;
        this.width = other.width;
        this.height = other.height;
    }
    
    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    public void setOffsetX(int offsetX) { this.offsetX = offsetX; }
    public void setOffsetY(int offsetY) { this.offsetY = offsetY; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    
    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public void resetOffset() {
        this.offsetX = defaultOffsetX;
        this.offsetY = defaultOffsetY;
    }
    
    public void resetSize() {
        this.width = defaultWidth;
        this.height = defaultHeight;
    }
    
    public void reset() {
        resetOffset();
        resetSize();
    }
    
    public int getDefaultOffsetX() { return defaultOffsetX; }
    public int getDefaultOffsetY() { return defaultOffsetY; }
    public int getDefaultWidth() { return defaultWidth; }
    public int getDefaultHeight() { return defaultHeight; }
}
