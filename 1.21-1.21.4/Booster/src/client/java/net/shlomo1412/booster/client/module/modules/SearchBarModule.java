package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterSearchField;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Module that adds a search bar to container screens.
 * Highlights items in both the container and player inventory that match the search query.
 */
public class SearchBarModule extends GUIModule {
    
    public static final String SEARCH_WIDGET_ID = "searchbar";
    
    // Module settings
    private final ModuleSetting.ColorSetting highlightColor;
    
    // Runtime state
    private BoosterSearchField searchField;
    private String currentQuery = "";
    private final Set<Integer> matchingSlots = new HashSet<>();
    private HandledScreen<?> currentScreen;
    
    public SearchBarModule() {
        super(
            "search_bar",
            "Search Bar",
            "Adds a search bar to filter and highlight items in containers.\n" +
            "Matching items will be highlighted with a customizable color.",
            true,
            120,  // Default width
            18    // Default height
        );
        
        // Initialize settings
        this.highlightColor = new ModuleSetting.ColorSetting(
            "highlight_color",
            "Highlight Color",
            "The color used to highlight matching items",
            0x8000FF00  // Semi-transparent green
        );
        
        // Register settings
        registerSetting(highlightColor);
    }
    
    /**
     * Creates the search bar widget for a container screen.
     * 
     * @param screen The container screen
     * @param anchorX Left edge of container
     * @param anchorY Top edge of container (will be used to calculate relative position)
     * @param containerWidth The width of the container
     * @param screenHeight The total screen height for clamping
     * @param addDrawableChild Callback to add the widget
     */
    public void createSearchBar(HandledScreen<?> screen, int anchorX, int anchorY, int containerWidth, int screenHeight,
                                 Consumer<BoosterSearchField> addDrawableChild) {
        this.currentScreen = screen;
        
        // Get widget settings - default: centered above container
        WidgetSettings settings = getWidgetSettings(SEARCH_WIDGET_ID, 0, -22);
        
        int searchX = anchorX + settings.getOffsetX();
        int searchY = anchorY + settings.getOffsetY();
        
        // Check if there's not enough space above container (e.g., double chest)
        // Minimum space needed: search bar height + some padding
        int minSpaceNeeded = settings.getHeight() + 4;
        if (searchY < minSpaceNeeded && settings.getOffsetY() < 0) {
            // Move search bar to the left of the container, but offset to avoid progress bar
            // Progress bar is typically at -12 offset, so place search bar further left
            searchX = anchorX - settings.getWidth() - 20;  // 20px gap to clear progress bar
            searchY = anchorY + 4;  // Small offset from top
            
            // If that would go off-screen to the left, try the right side
            if (searchX < 2) {
                searchX = anchorX + containerWidth + 4;
            }
        }
        
        // Final clamp to ensure visibility
        searchY = Math.max(2, searchY);
        
        searchField = new BoosterSearchField(
            searchX, searchY,
            settings.getWidth(), settings.getHeight(),
            Text.literal("Search...")
        );
        searchField.setPlaceholder(Text.literal("Search items...").styled(s -> s.withColor(0x666666)));
        searchField.setMaxLength(50);
        searchField.setDrawsBackground(true);
        searchField.setChangedListener(this::onSearchChanged);
        searchField.setEditable(true);
        
        // Set editor info for drag support
        searchField.setEditorInfo(this, SEARCH_WIDGET_ID, "Search Bar", anchorX, anchorY);
        
        addDrawableChild.accept(searchField);
    }
    
    /**
     * Called when the search text changes.
     */
    private void onSearchChanged(String query) {
        this.currentQuery = query.toLowerCase().trim();
        updateMatchingSlots();
    }
    
    /**
     * Updates the set of matching slot indices.
     */
    private void updateMatchingSlots() {
        matchingSlots.clear();
        
        if (currentQuery.isEmpty() || currentScreen == null) {
            return;
        }
        
        var handler = currentScreen.getScreenHandler();
        for (Slot slot : handler.slots) {
            if (slot.hasStack()) {
                ItemStack stack = slot.getStack();
                String itemName = stack.getName().getString().toLowerCase();
                
                // Check if item name contains search query
                if (itemName.contains(currentQuery)) {
                    matchingSlots.add(slot.id);
                }
            }
        }
    }
    
    /**
     * Renders the highlight overlay for matching slots.
     * Call this after slots are rendered but before tooltips.
     */
    public void renderHighlights(DrawContext context, int containerX, int containerY) {
        if (currentQuery.isEmpty() || currentScreen == null || matchingSlots.isEmpty()) {
            return;
        }
        
        int color = highlightColor.getValue();
        
        var handler = currentScreen.getScreenHandler();
        for (Slot slot : handler.slots) {
            if (matchingSlots.contains(slot.id)) {
                int slotX = containerX + slot.x;
                int slotY = containerY + slot.y;
                
                // Draw highlight overlay
                context.fill(slotX, slotY, slotX + 16, slotY + 16, color);
                
                // Draw border
                int borderColor = (color & 0x00FFFFFF) | 0xFF000000;  // Full opacity border
                context.fill(slotX, slotY, slotX + 16, slotY + 1, borderColor);           // Top
                context.fill(slotX, slotY + 15, slotX + 16, slotY + 16, borderColor);     // Bottom
                context.fill(slotX, slotY, slotX + 1, slotY + 16, borderColor);           // Left
                context.fill(slotX + 15, slotY, slotX + 16, slotY + 16, borderColor);     // Right
            }
        }
    }
    
    /**
     * Dims non-matching slots when search is active.
     */
    public void renderSlotDimming(DrawContext context, int containerX, int containerY) {
        if (currentQuery.isEmpty() || currentScreen == null) {
            return;
        }
        
        var handler = currentScreen.getScreenHandler();
        for (Slot slot : handler.slots) {
            // Only dim slots that have items but don't match
            if (slot.hasStack() && !matchingSlots.contains(slot.id)) {
                int slotX = containerX + slot.x;
                int slotY = containerY + slot.y;
                
                // Draw semi-transparent dark overlay
                context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x80000000);
            }
        }
    }
    
    /**
     * @return true if search is active and has a query
     */
    public boolean isSearchActive() {
        return !currentQuery.isEmpty();
    }
    
    /**
     * @return The current search query
     */
    public String getCurrentQuery() {
        return currentQuery;
    }
    
    /**
     * @return The number of matching items
     */
    public int getMatchCount() {
        return matchingSlots.size();
    }
    
    /**
     * Gets the search field widget.
     */
    public BoosterSearchField getSearchField() {
        return searchField;
    }
    
    /**
     * Clears the search and resets state.
     */
    public void clearSearch() {
        if (searchField != null) {
            searchField.setText("");
        }
        currentQuery = "";
        matchingSlots.clear();
    }
    
    /**
     * Gets the highlight color setting.
     */
    public ModuleSetting.ColorSetting getHighlightColorSetting() {
        return highlightColor;
    }
    
    @Override
    protected void onEnable() {
        // Nothing special needed
    }
    
    @Override
    protected void onDisable() {
        searchField = null;
        currentScreen = null;
        currentQuery = "";
        matchingSlots.clear();
    }
}
