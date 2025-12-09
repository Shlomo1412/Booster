package net.shlomo1412.booster.client.module.modules;

/**
 * Enum representing different sorting modes for inventory/container sorting.
 */
public enum SortMode {
    NAME("Name", "Sort items alphabetically by name"),
    COUNT("Count", "Sort items by stack count (highest first)"),
    CREATIVE_GROUPS("Creative Tab", "Sort items by their creative tab category"),
    ROWS("Rows", "Organize items into rows, filling left to right"),
    COLUMNS("Columns", "Organize items into columns, filling top to bottom"),
    RAW_ID("Item ID", "Sort items by their raw registry ID");
    
    private final String displayName;
    private final String description;
    
    SortMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * @return The display name shown in UI
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * @return The description of this sort mode
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the next sort mode in the cycle.
     */
    public SortMode next() {
        SortMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
    
    /**
     * Gets the previous sort mode in the cycle.
     */
    public SortMode previous() {
        SortMode[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
