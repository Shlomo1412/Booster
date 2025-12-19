package net.shlomo1412.booster.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.shlomo1412.booster.client.module.AlertModule;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.Module;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.ModuleSetting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Beautiful configuration screen for Booster mod.
 * Features tabbed categories, module toggles, and detailed descriptions.
 */
public class BoosterConfigScreen extends Screen {
    
    // Layout constants
    private static final int HEADER_HEIGHT = 50;
    private static final int TAB_HEIGHT = 28;
    private static final int TAB_WIDTH = 100;
    private static final int SIDEBAR_WIDTH = 220;  // Increased for more room
    private static final int CONTENT_PADDING = 16;
    private static final int MODULE_CARD_HEIGHT = 80;
    private static final int MODULE_CARD_SPACING = 8;
    private static final int SEARCH_BAR_HEIGHT = 24;
    
    // Colors
    private static final int BG_COLOR = 0xFF0D0D0D;
    private static final int HEADER_BG = 0xFF1A1A1A;
    private static final int TAB_BG = 0xFF222222;
    private static final int TAB_ACTIVE = 0xFF333333;
    private static final int TAB_HOVER = 0xFF2A2A2A;
    private static final int CARD_BG = 0xFF1A1A1A;
    private static final int CARD_HOVER = 0xFF222222;
    private static final int CARD_BORDER = 0xFF333333;
    private static final int ACCENT_COLOR = 0xFFFFAA00;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFAAAAAA;
    private static final int TEXT_DIM = 0xFF666666;
    private static final int ENABLED_COLOR = 0xFF44BB44;
    private static final int DISABLED_COLOR = 0xFF666666;
    
    // Setting control dimensions
    private static final int SETTING_HEIGHT = 24;
    private static final int SETTING_BUTTON_WIDTH = 80;
    private static final int SETTING_TOGGLE_WIDTH = 40;
    private static final int COLOR_PREVIEW_SIZE = 16;
    
    // Tabs/Categories
    private final Map<String, List<Module>> categories = new LinkedHashMap<>();
    private String activeCategory = "All";
    
    // Scrolling for module list
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private float maxScrollOffset = 0f;
    private static final float SCROLL_SPEED = 0.25f;
    private static final int SCROLL_AMOUNT = 30;
    
    // Scrolling for details panel
    private float detailScrollOffset = 0f;
    private float targetDetailScrollOffset = 0f;
    private float maxDetailScrollOffset = 0f;
    
    // Selected module for details panel
    private Module selectedModule = null;
    
    // Interactive setting controls
    private final List<SettingControl> settingControls = new ArrayList<>();
    private ModuleSetting.ColorSetting editingColorSetting = null;
    private int colorPickerX, colorPickerY;
    
    // Search functionality
    private String searchQuery = "";
    private List<Module> filteredModules = new ArrayList<>();
    private boolean searchBarFocused = false;
    private int searchBarX, searchBarY, searchBarWidth;
    
    // Animation
    private float openAnimation = 0f;
    
    // Parent screen to return to
    private final Screen parent;
    
    public BoosterConfigScreen(Screen parent) {
        super(Text.literal("Booster Configuration"));
        this.parent = parent;
        initializeCategories();
        updateFilteredModules();
    }
    
    private void updateFilteredModules() {
        filteredModules.clear();
        List<Module> categoryModules = categories.get(activeCategory);
        if (categoryModules == null) return;
        
        if (searchQuery.isEmpty()) {
            filteredModules.addAll(categoryModules);
        } else {
            String query = searchQuery.toLowerCase();
            for (Module module : categoryModules) {
                if (module.getName().toLowerCase().contains(query) ||
                    module.getId().toLowerCase().contains(query) ||
                    module.getDescription().toLowerCase().contains(query)) {
                    filteredModules.add(module);
                }
            }
        }
    }
    
    private void initializeCategories() {
        // Add "All" category
        List<Module> allModules = new ArrayList<>(ModuleManager.getInstance().getModules());
        categories.put("All", allModules);
        
        // Add "GUI" category for GUI modules
        List<Module> guiModules = new ArrayList<>();
        for (Module module : allModules) {
            if (module instanceof GUIModule) {
                guiModules.add(module);
            }
        }
        if (!guiModules.isEmpty()) {
            categories.put("GUI", guiModules);
        }
        
        // Add "Utility" category for non-GUI modules
        List<Module> utilityModules = new ArrayList<>();
        for (Module module : allModules) {
            if (!(module instanceof GUIModule)) {
                utilityModules.add(module);
            }
        }
        if (!utilityModules.isEmpty()) {
            categories.put("Utility", utilityModules);
        }
        
        // Select first module by default
        if (!allModules.isEmpty()) {
            selectedModule = allModules.get(0);
        }
    }
    
    @Override
    protected void init() {
        // Close button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("✕"), button -> close())
            .dimensions(this.width - 30, 10, 20, 20)
            .build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update animations
        openAnimation = MathHelper.lerp(0.15f, openAnimation, 1f);
        scrollOffset = MathHelper.lerp(SCROLL_SPEED, scrollOffset, targetScrollOffset);
        detailScrollOffset = MathHelper.lerp(SCROLL_SPEED, detailScrollOffset, targetDetailScrollOffset);
        
        // Solid background (no blur effect)
        context.fill(0, 0, this.width, this.height, BG_COLOR);
        
        // Header
        renderHeader(context, mouseX, mouseY);
        
        // Tabs
        renderTabs(context, mouseX, mouseY);
        
        // Main content area
        int contentY = HEADER_HEIGHT + TAB_HEIGHT;
        int contentHeight = this.height - contentY;
        
        // Module list (left side)
        int listWidth = this.width - SIDEBAR_WIDTH - CONTENT_PADDING * 2;
        renderModuleList(context, CONTENT_PADDING, contentY, listWidth, contentHeight, mouseX, mouseY);
        
        // Details panel (right side)
        renderDetailsPanel(context, this.width - SIDEBAR_WIDTH - CONTENT_PADDING, contentY, 
                          SIDEBAR_WIDTH, contentHeight, mouseX, mouseY);
        
        // Render widgets (buttons) - don't call super.render to avoid background blur
        for (var element : this.children()) {
            if (element instanceof net.minecraft.client.gui.Drawable drawable) {
                drawable.render(context, mouseX, mouseY, delta);
            }
        }
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Override to prevent default blur/gradient background
        context.fill(0, 0, this.width, this.height, BG_COLOR);
    }
    
    private void renderHeader(DrawContext context, int mouseX, int mouseY) {
        // Header background
        context.fill(0, 0, this.width, HEADER_HEIGHT, HEADER_BG);
        
        // Accent line at bottom
        context.fill(0, HEADER_HEIGHT - 2, this.width, HEADER_HEIGHT, ACCENT_COLOR);
        
        // Title
        context.drawTextWithShadow(this.textRenderer, 
            Text.literal("⚡ BOOSTER").formatted(Formatting.GOLD, Formatting.BOLD),
            CONTENT_PADDING, 12, TEXT_PRIMARY);
        
        // Subtitle
        context.drawTextWithShadow(this.textRenderer,
            Text.literal("Configuration").formatted(Formatting.GRAY),
            CONTENT_PADDING, 26, TEXT_SECONDARY);
        
        // Module count
        int enabledCount = 0;
        for (Module m : ModuleManager.getInstance().getModules()) {
            if (m.isEnabled()) enabledCount++;
        }
        int totalCount = ModuleManager.getInstance().getModules().size();
        String countText = enabledCount + "/" + totalCount + " modules enabled";
        int countWidth = this.textRenderer.getWidth(countText);
        
        // Search bar in header (next to module count)
        searchBarWidth = 180;
        searchBarX = this.width - countWidth - 60 - searchBarWidth;
        searchBarY = 13;
        renderSearchBar(context, searchBarX, searchBarY, searchBarWidth, mouseX, mouseY);
        
        context.drawTextWithShadow(this.textRenderer, countText, 
            this.width - countWidth - 40, 20, TEXT_DIM);
    }
    
    private void renderTabs(DrawContext context, int mouseX, int mouseY) {
        int tabY = HEADER_HEIGHT;
        int tabX = 0;
        
        // Tab bar background
        context.fill(0, tabY, this.width, tabY + TAB_HEIGHT, TAB_BG);
        
        for (String category : categories.keySet()) {
            boolean isActive = category.equals(activeCategory);
            boolean isHovered = mouseX >= tabX && mouseX < tabX + TAB_WIDTH &&
                               mouseY >= tabY && mouseY < tabY + TAB_HEIGHT;
            
            // Tab background
            int tabColor = isActive ? TAB_ACTIVE : (isHovered ? TAB_HOVER : TAB_BG);
            context.fill(tabX, tabY, tabX + TAB_WIDTH, tabY + TAB_HEIGHT, tabColor);
            
            // Active indicator
            if (isActive) {
                context.fill(tabX, tabY + TAB_HEIGHT - 3, tabX + TAB_WIDTH, tabY + TAB_HEIGHT, ACCENT_COLOR);
            }
            
            // Tab text
            String tabText = category + " (" + categories.get(category).size() + ")";
            int textWidth = this.textRenderer.getWidth(tabText);
            int textColor = isActive ? TEXT_PRIMARY : TEXT_SECONDARY;
            context.drawTextWithShadow(this.textRenderer, tabText,
                tabX + (TAB_WIDTH - textWidth) / 2, tabY + 10, textColor);
            
            // Tab separator
            context.fill(tabX + TAB_WIDTH - 1, tabY + 4, tabX + TAB_WIDTH, tabY + TAB_HEIGHT - 4, CARD_BORDER);
            
            tabX += TAB_WIDTH;
        }
    }
    
    private void renderModuleList(DrawContext context, int x, int y, int width, int height, 
                                   int mouseX, int mouseY) {
        // Module list starts at y (search bar is now in header)
        int listY = y;
        int listHeight = height;
        
        if (filteredModules.isEmpty()) {
            String msg = searchQuery.isEmpty() ? "No modules in this category" : "No modules match \"" + searchQuery + "\"";
            context.drawCenteredTextWithShadow(this.textRenderer, msg,
                x + width / 2, listY + listHeight / 2, TEXT_DIM);
            return;
        }
        
        // Calculate total content height and max scroll
        int totalHeight = filteredModules.size() * (MODULE_CARD_HEIGHT + MODULE_CARD_SPACING);
        maxScrollOffset = Math.max(0, totalHeight - listHeight + CONTENT_PADDING * 2);
        
        // Scissor for clipping
        context.enableScissor(x, listY, x + width, listY + listHeight);
        
        int cardY = listY + CONTENT_PADDING - (int) scrollOffset;
        
        for (Module module : filteredModules) {
            if (cardY + MODULE_CARD_HEIGHT >= listY && cardY < listY + listHeight) {
                renderModuleCard(context, x, cardY, width - CONTENT_PADDING, module, mouseX, mouseY);
            }
            cardY += MODULE_CARD_HEIGHT + MODULE_CARD_SPACING;
        }
        
        context.disableScissor();
        
        // Scroll indicators
        if (scrollOffset > 0) {
            // Top fade
            for (int i = 0; i < 20; i++) {
                int alpha = (int) ((1 - i / 20f) * 200);
                context.fill(x, listY + i, x + width, listY + i + 1, (alpha << 24) | 0x0D0D0D);
            }
        }
        if (scrollOffset < maxScrollOffset) {
            // Bottom fade
            for (int i = 0; i < 20; i++) {
                int alpha = (int) ((1 - i / 20f) * 200);
                context.fill(x, listY + listHeight - i - 1, x + width, listY + listHeight - i, (alpha << 24) | 0x0D0D0D);
            }
        }
    }
    
    /**
     * Renders the search bar.
     */
    private void renderSearchBar(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
        // Background
        boolean hovered = mouseX >= x && mouseX < x + width &&
                         mouseY >= y && mouseY < y + SEARCH_BAR_HEIGHT;
        int bg = searchBarFocused ? 0xFF333333 : (hovered ? 0xFF2A2A2A : 0xFF1A1A1A);
        int borderColor = searchBarFocused ? ACCENT_COLOR : 0xFF444444;
        context.fill(x, y, x + width, y + SEARCH_BAR_HEIGHT, bg);
        drawBorder(context, x, y, width, SEARCH_BAR_HEIGHT, borderColor);
        
        // Search icon
        context.drawTextWithShadow(this.textRenderer, "🔍", x + 4, y + 7, searchBarFocused ? ACCENT_COLOR : TEXT_DIM);
        
        // Search text or placeholder
        int textX = x + 18;
        if (searchQuery.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, "Search...", textX, y + 7, TEXT_DIM);
        } else {
            String displayText = trimTextToWidth(searchQuery, width - 36);
            context.drawTextWithShadow(this.textRenderer, displayText, textX, y + 7, TEXT_PRIMARY);
            
            // Clear button
            int clearX = x + width - 14;
            boolean clearHovered = mouseX >= clearX && mouseX < clearX + 12 &&
                                   mouseY >= y && mouseY < y + SEARCH_BAR_HEIGHT;
            int clearColor = clearHovered ? 0xFFFF5555 : TEXT_DIM;
            context.drawTextWithShadow(this.textRenderer, "✕", clearX, y + 7, clearColor);
        }
        
        // Blinking cursor when focused
        if (searchBarFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = textX + this.textRenderer.getWidth(searchQuery);
            context.fill(cursorX, y + 5, cursorX + 1, y + SEARCH_BAR_HEIGHT - 5, TEXT_PRIMARY);
        }
    }
    
    private void renderModuleCard(DrawContext context, int x, int y, int width, Module module, 
                                   int mouseX, int mouseY) {
        boolean isHovered = mouseX >= x && mouseX < x + width &&
                           mouseY >= y && mouseY < y + MODULE_CARD_HEIGHT;
        boolean isSelected = module == selectedModule;
        
        // Card background
        int bgColor = isSelected ? CARD_HOVER : (isHovered ? CARD_HOVER : CARD_BG);
        context.fill(x, y, x + width, y + MODULE_CARD_HEIGHT, bgColor);
        
        // Left accent bar
        int accentColor = module.isEnabled() ? ENABLED_COLOR : DISABLED_COLOR;
        context.fill(x, y, x + 4, y + MODULE_CARD_HEIGHT, accentColor);
        
        // Selected border
        if (isSelected) {
            context.fill(x, y, x + width, y + 1, ACCENT_COLOR);
            context.fill(x, y + MODULE_CARD_HEIGHT - 1, x + width, y + MODULE_CARD_HEIGHT, ACCENT_COLOR);
            context.fill(x + width - 1, y, x + width, y + MODULE_CARD_HEIGHT, ACCENT_COLOR);
        }
        
        // Module name
        context.drawTextWithShadow(this.textRenderer,
            Text.literal(module.getName()).formatted(Formatting.WHITE, Formatting.BOLD),
            x + 12, y + 8, TEXT_PRIMARY);
        
        // Module ID
        context.drawTextWithShadow(this.textRenderer,
            Text.literal(module.getId()).formatted(Formatting.DARK_GRAY),
            x + 12, y + 22, TEXT_DIM);
        
        // Status badge
        String status = module.isEnabled() ? "ENABLED" : "DISABLED";
        int statusColor = module.isEnabled() ? ENABLED_COLOR : DISABLED_COLOR;
        int statusWidth = this.textRenderer.getWidth(status);
        context.fill(x + width - statusWidth - 16, y + 6, x + width - 8, y + 18, 
                    module.isEnabled() ? 0xFF1A3A1A : 0xFF3A1A1A);
        context.drawTextWithShadow(this.textRenderer, status, x + width - statusWidth - 12, y + 8, statusColor);
        
        // Description preview (first line) - fit within card width
        String desc = module.getDescription();
        if (desc.contains("\n")) {
            desc = desc.substring(0, desc.indexOf("\n"));
        }
        // Calculate max width for text (card width - left padding - right padding)
        int maxTextWidth = width - 24;
        desc = trimTextToWidth(desc, maxTextWidth);
        context.drawTextWithShadow(this.textRenderer, desc, x + 12, y + 40, TEXT_SECONDARY);
        
        // Type badge
        String type = module instanceof GUIModule ? "GUI" : "UTIL";
        int typeColor = module instanceof GUIModule ? 0xFF5588FF : 0xFFAA55FF;
        context.drawTextWithShadow(this.textRenderer, "[" + type + "]", x + 12, y + 56, typeColor);
        
        // Toggle button
        int toggleX = x + width - 50;
        int toggleY = y + MODULE_CARD_HEIGHT - 28;
        int toggleW = 42;
        int toggleH = 20;
        
        boolean toggleHovered = mouseX >= toggleX && mouseX < toggleX + toggleW &&
                               mouseY >= toggleY && mouseY < toggleY + toggleH;
        
        int toggleBg = module.isEnabled() ? 
            (toggleHovered ? 0xFF2A5A2A : 0xFF1A4A1A) : 
            (toggleHovered ? 0xFF4A2A2A : 0xFF3A1A1A);
        int toggleBorder = module.isEnabled() ? 0xFF44AA44 : 0xFFAA4444;
        
        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, toggleBg);
        context.fill(toggleX, toggleY, toggleX + toggleW, toggleY + 1, toggleBorder);
        context.fill(toggleX, toggleY + toggleH - 1, toggleX + toggleW, toggleY + toggleH, toggleBorder);
        context.fill(toggleX, toggleY, toggleX + 1, toggleY + toggleH, toggleBorder);
        context.fill(toggleX + toggleW - 1, toggleY, toggleX + toggleW, toggleY + toggleH, toggleBorder);
        
        String toggleText = module.isEnabled() ? "ON" : "OFF";
        int toggleTextWidth = this.textRenderer.getWidth(toggleText);
        context.drawTextWithShadow(this.textRenderer, toggleText,
            toggleX + (toggleW - toggleTextWidth) / 2, toggleY + 6, TEXT_PRIMARY);
    }
    
    private void renderDetailsPanel(DrawContext context, int x, int y, int width, int height,
                                     int mouseX, int mouseY) {
        // Panel background
        context.fill(x, y, x + width, y + height, CARD_BG);
        context.fill(x, y, x + 1, y + height, CARD_BORDER);
        
        if (selectedModule == null) {
            context.drawCenteredTextWithShadow(this.textRenderer, "Select a module",
                x + width / 2, y + height / 2, TEXT_DIM);
            maxDetailScrollOffset = 0;
            return;
        }
        
        int contentX = x + CONTENT_PADDING;
        int contentStartY = y + CONTENT_PADDING;
        int contentWidth = width - CONTENT_PADDING * 2;
        
        // Enable scissor for scrolling content
        context.enableScissor(x, y, x + width, y + height);
        
        // Apply scroll offset
        int contentY = contentStartY - (int) detailScrollOffset;
        
        // Module name
        context.drawTextWithShadow(this.textRenderer,
            Text.literal(selectedModule.getName()).formatted(Formatting.GOLD, Formatting.BOLD),
            contentX, contentY, ACCENT_COLOR);
        contentY += 16;
        
        // Module ID
        context.drawTextWithShadow(this.textRenderer,
            Text.literal("ID: " + selectedModule.getId()).formatted(Formatting.DARK_GRAY),
            contentX, contentY, TEXT_DIM);
        contentY += 20;
        
        // Divider
        context.fill(contentX, contentY, contentX + contentWidth, contentY + 1, CARD_BORDER);
        contentY += 10;
        
        // Description header
        context.drawTextWithShadow(this.textRenderer,
            Text.literal("Description").formatted(Formatting.WHITE),
            contentX, contentY, TEXT_PRIMARY);
        contentY += 14;
        
        // Description (word wrapped)
        String[] descLines = selectedModule.getDescription().split("\n");
        for (String line : descLines) {
            // Simple word wrap
            List<String> wrapped = wrapText(line, contentWidth);
            for (String wrappedLine : wrapped) {
                context.drawTextWithShadow(this.textRenderer, wrappedLine, contentX, contentY, TEXT_SECONDARY);
                contentY += 12;
            }
        }
        contentY += 10;
        
        // Divider
        context.fill(contentX, contentY, contentX + contentWidth, contentY + 1, CARD_BORDER);
        contentY += 10;
        
        // Status
        context.drawTextWithShadow(this.textRenderer,
            Text.literal("Status").formatted(Formatting.WHITE),
            contentX, contentY, TEXT_PRIMARY);
        contentY += 14;
        
        String statusText = selectedModule.isEnabled() ? "● Enabled" : "○ Disabled";
        int statusColor = selectedModule.isEnabled() ? ENABLED_COLOR : DISABLED_COLOR;
        context.drawTextWithShadow(this.textRenderer, statusText, contentX, contentY, statusColor);
        contentY += 20;
        
        // Clear setting controls - they'll be rebuilt
        settingControls.clear();
        
        // Interactive settings for AlertModule
        if (selectedModule instanceof AlertModule alertModule && alertModule.hasSettings()) {
            context.fill(contentX, contentY, contentX + contentWidth, contentY + 1, CARD_BORDER);
            contentY += 10;
            
            context.drawTextWithShadow(this.textRenderer,
                Text.literal("⚙ Settings").formatted(Formatting.WHITE, Formatting.BOLD),
                contentX, contentY, TEXT_PRIMARY);
            contentY += 18;
            
            // Render each setting with interactive controls
            for (var setting : alertModule.getSettings()) {
                contentY = renderSettingControl(context, contentX, contentY, contentWidth, 
                    setting, alertModule, mouseX, mouseY);
            }
            contentY += 10;
        }
        
        // GUI Module specific info
        if (selectedModule instanceof GUIModule guiModule) {
            context.fill(contentX, contentY, contentX + contentWidth, contentY + 1, CARD_BORDER);
            contentY += 10;
            
            context.drawTextWithShadow(this.textRenderer,
                Text.literal("Widgets").formatted(Formatting.WHITE),
                contentX, contentY, TEXT_PRIMARY);
            contentY += 14;
            
            // Show each widget's settings
            for (String widgetId : guiModule.getWidgetIds()) {
                var settings = guiModule.getWidgetSettings(widgetId);
                context.drawTextWithShadow(this.textRenderer,
                    widgetId + ":",
                    contentX, contentY, TEXT_SECONDARY);
                contentY += 12;
                context.drawTextWithShadow(this.textRenderer,
                    "  Pos: " + settings.getOffsetX() + ", " + settings.getOffsetY() +
                    "  Size: " + settings.getWidth() + "×" + settings.getHeight(),
                    contentX, contentY, TEXT_DIM);
                contentY += 14;
            }
            contentY += 6;
            
            // Show module settings if available (for GUI modules, still show as read-only)
            if (guiModule.hasSettings()) {
                context.fill(contentX, contentY, contentX + contentWidth, contentY + 1, CARD_BORDER);
                contentY += 10;
                
                context.drawTextWithShadow(this.textRenderer,
                    Text.literal("Settings").formatted(Formatting.WHITE),
                    contentX, contentY, TEXT_PRIMARY);
                contentY += 14;
                
                context.drawTextWithShadow(this.textRenderer,
                    Text.literal("(Edit in Editor Mode)").formatted(Formatting.ITALIC),
                    contentX, contentY, TEXT_DIM);
                contentY += 14;
                
                for (var setting : guiModule.getSettings()) {
                    String settingName = setting.getName();
                    String settingValue = formatSettingValue(setting);
                    context.drawTextWithShadow(this.textRenderer,
                        settingName + ":",
                        contentX, contentY, TEXT_SECONDARY);
                    contentY += 12;
                    context.drawTextWithShadow(this.textRenderer,
                        "  " + settingValue,
                        contentX, contentY, 0xFF88CCFF);
                    contentY += 14;
                }
                contentY += 6;
            }
        }
        
        // Type
        String type = selectedModule instanceof GUIModule ? "GUI Module" : "Utility Module";
        int typeColor = selectedModule instanceof GUIModule ? 0xFF5588FF : 0xFFAA55FF;
        context.drawTextWithShadow(this.textRenderer, "Type: " + type, contentX, contentY, typeColor);
        contentY += 20;
        
        // Disable scissor
        context.disableScissor();
        
        // Calculate max scroll offset based on content height
        int totalContentHeight = contentY + (int) detailScrollOffset - contentStartY;
        maxDetailScrollOffset = Math.max(0, totalContentHeight - height + CONTENT_PADDING * 2);
        
        // Reset scroll when selecting new module
        if (targetDetailScrollOffset > maxDetailScrollOffset) {
            targetDetailScrollOffset = maxDetailScrollOffset;
        }
    }
    
    /**
     * Formats a module setting value for display.
     */
    private String formatSettingValue(net.shlomo1412.booster.client.module.ModuleSetting<?> setting) {
        Object value = setting.getValue();
        if (value instanceof Integer color && setting instanceof net.shlomo1412.booster.client.module.ModuleSetting.ColorSetting) {
            // Format as hex color
            return String.format("#%06X", color & 0xFFFFFF);
        } else if (value instanceof Enum<?> enumValue) {
            return enumValue.toString();
        } else if (value instanceof Boolean bool) {
            return bool ? "Yes" : "No";
        } else if (value instanceof Number num) {
            // Format with reasonable precision
            if (value instanceof Float || value instanceof Double) {
                return String.format("%.2f", num.doubleValue());
            }
            return num.toString();
        }
        return String.valueOf(value);
    }
    
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : text.split(" ")) {
            String test = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (this.textRenderer.getWidth(test) > maxWidth) {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            } else {
                if (!currentLine.isEmpty()) currentLine.append(" ");
                currentLine.append(word);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle search bar click
        if (mouseX >= searchBarX && mouseX < searchBarX + searchBarWidth &&
            mouseY >= searchBarY && mouseY < searchBarY + SEARCH_BAR_HEIGHT) {
            
            // Check for clear button click
            if (!searchQuery.isEmpty()) {
                int clearX = searchBarX + searchBarWidth - 14;
                if (mouseX >= clearX && mouseX < clearX + 12) {
                    searchQuery = "";
                    updateFilteredModules();
                    return true;
                }
            }
            
            searchBarFocused = true;
            return true;
        } else {
            searchBarFocused = false;
        }
        
        // Handle setting control clicks first (in details panel)
        if (handleSettingClick(mouseX, mouseY)) {
            return true;
        }
        
        // Tab clicks
        int tabY = HEADER_HEIGHT;
        int tabX = 0;
        for (String category : categories.keySet()) {
            if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH &&
                mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
                activeCategory = category;
                scrollOffset = 0;
                targetScrollOffset = 0;
                searchQuery = ""; // Clear search on tab change
                updateFilteredModules(); // Update the filtered list for new category
                return true;
            }
            tabX += TAB_WIDTH;
        }
        
        // Module card clicks - use filtered modules
        int contentY = HEADER_HEIGHT + TAB_HEIGHT;
        int contentHeight = this.height - contentY;
        int listWidth = this.width - SIDEBAR_WIDTH - CONTENT_PADDING * 2;
        
        if (filteredModules != null && !filteredModules.isEmpty()) {
            int cardY = contentY + CONTENT_PADDING - (int) scrollOffset;
            
            for (Module module : filteredModules) {
                int cardX = CONTENT_PADDING;
                int cardWidth = listWidth - CONTENT_PADDING;
                
                if (mouseY >= cardY && mouseY < cardY + MODULE_CARD_HEIGHT &&
                    mouseX >= cardX && mouseX < cardX + cardWidth &&
                    mouseY >= contentY && mouseY < contentY + contentHeight) {
                    
                    // Check toggle button click
                    int toggleX = cardX + cardWidth - 50;
                    int toggleY = cardY + MODULE_CARD_HEIGHT - 28;
                    if (mouseX >= toggleX && mouseX < toggleX + 42 &&
                        mouseY >= toggleY && mouseY < toggleY + 20) {
                        module.setEnabled(!module.isEnabled());
                        return true;
                    }
                    
                    // Select module
                    selectedModule = module;
                    // Reset details scroll when selecting a new module
                    targetDetailScrollOffset = 0;
                    detailScrollOffset = 0;
                    return true;
                }
                
                cardY += MODULE_CARD_HEIGHT + MODULE_CARD_SPACING;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchBarFocused) {
            if (chr >= ' ') { // Printable characters
                searchQuery += chr;
                scrollOffset = 0; // Reset scroll when searching
                updateFilteredModules(); // Update results
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBarFocused) {
            if (keyCode == 259) { // Backspace
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    updateFilteredModules(); // Update results
                }
                return true;
            } else if (keyCode == 256) { // Escape
                if (!searchQuery.isEmpty()) {
                    searchQuery = "";
                    updateFilteredModules(); // Update results
                    return true;
                } else {
                    searchBarFocused = false;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Only scroll if mouse is over the module list area (left side)
        int contentY = HEADER_HEIGHT + TAB_HEIGHT;
        int listWidth = this.width - SIDEBAR_WIDTH - CONTENT_PADDING * 2;
        
        if (mouseX < listWidth + CONTENT_PADDING) {
            // Over module list - scroll it
            targetScrollOffset -= verticalAmount * SCROLL_AMOUNT;
            targetScrollOffset = MathHelper.clamp(targetScrollOffset, 0, maxScrollOffset);
            return true;
        } else {
            // Over details panel - scroll details (currently details panel isn't scrollable,
            // but we prevent scrolling the module list when hovering over details)
            targetDetailScrollOffset -= verticalAmount * SCROLL_AMOUNT;
            targetDetailScrollOffset = MathHelper.clamp(targetDetailScrollOffset, 0, maxDetailScrollOffset);
            return true;
        }
    }
    
    /**
     * Trims text to fit within a given pixel width, adding ellipsis if needed.
     */
    private String trimTextToWidth(String text, int maxWidth) {
        if (this.textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = this.textRenderer.getWidth(ellipsis);
        int targetWidth = maxWidth - ellipsisWidth;
        
        StringBuilder trimmed = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (this.textRenderer.getWidth(trimmed.toString() + c) > targetWidth) {
                break;
            }
            trimmed.append(c);
        }
        return trimmed.toString() + ellipsis;
    }
    
    @Override
    public void close() {
        this.client.setScreen(parent);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    /**
     * Renders an interactive setting control and returns the new Y position.
     * Uses stacked layout: name on top, control below.
     */
    private int renderSettingControl(DrawContext context, int x, int y, int width,
                                      ModuleSetting<?> setting, AlertModule module,
                                      int mouseX, int mouseY) {
        // Setting name on its own line
        context.drawTextWithShadow(this.textRenderer, setting.getName(), x, y, TEXT_SECONDARY);
        
        // Control on the next line
        int controlY = y + 12;
        int controlX = x;
        int controlWidth = width - 8; // Full width for controls
        
        // Render based on setting type
        if (setting instanceof ModuleSetting.BooleanSetting boolSetting) {
            // Toggle button - full width
            boolean value = boolSetting.getValue();
            int toggleBg = value ? 0xFF2A5A2A : 0xFF4A2A2A;
            int toggleBorder = value ? 0xFF44AA44 : 0xFFAA4444;
            boolean hovered = mouseX >= controlX && mouseX < controlX + controlWidth &&
                             mouseY >= controlY && mouseY < controlY + SETTING_HEIGHT;
            
            if (hovered) toggleBg = value ? 0xFF3A6A3A : 0xFF5A3A3A;
            
            context.fill(controlX, controlY, controlX + controlWidth, controlY + SETTING_HEIGHT, toggleBg);
            drawBorder(context, controlX, controlY, controlWidth, SETTING_HEIGHT, toggleBorder);
            
            String toggleText = value ? "ON" : "OFF";
            int textWidth = this.textRenderer.getWidth(toggleText);
            context.drawTextWithShadow(this.textRenderer, toggleText,
                controlX + (controlWidth - textWidth) / 2, controlY + 8, TEXT_PRIMARY);
            
            settingControls.add(new SettingControl(setting, module, controlX, controlY, 
                controlWidth, SETTING_HEIGHT, SettingControlType.BOOLEAN));
                
        } else if (setting instanceof ModuleSetting.EnumSetting<?> enumSetting) {
            // Dropdown-style button (cycles on click) - full width
            String valueText = enumSetting.getValue().toString();
            valueText = trimTextToWidth(valueText, controlWidth - 20);
            
            boolean hovered = mouseX >= controlX && mouseX < controlX + controlWidth &&
                             mouseY >= controlY && mouseY < controlY + SETTING_HEIGHT;
            int bg = hovered ? 0xFF3A3A4A : 0xFF2A2A3A;
            
            context.fill(controlX, controlY, controlX + controlWidth, controlY + SETTING_HEIGHT, bg);
            drawBorder(context, controlX, controlY, controlWidth, SETTING_HEIGHT, 0xFF5588FF);
            
            int textWidth = this.textRenderer.getWidth(valueText);
            context.drawTextWithShadow(this.textRenderer, valueText,
                controlX + (controlWidth - textWidth) / 2, controlY + 8, 0xFF88CCFF);
            
            // Arrow indicator
            context.drawTextWithShadow(this.textRenderer, "▼", 
                controlX + controlWidth - 12, controlY + 8, TEXT_DIM);
            
            settingControls.add(new SettingControl(setting, module, controlX, controlY,
                controlWidth, SETTING_HEIGHT, SettingControlType.ENUM));
                
        } else if (setting instanceof ModuleSetting.NumberSetting numSetting) {
            // Number with +/- buttons - full width
            int value = numSetting.getValue();
            int btnSize = SETTING_HEIGHT;
            int numWidth = controlWidth - btnSize * 2 - 4;
            
            // Minus button
            boolean minusHovered = mouseX >= controlX && mouseX < controlX + btnSize &&
                                   mouseY >= controlY && mouseY < controlY + SETTING_HEIGHT;
            int minusBg = minusHovered ? 0xFF4A3A3A : 0xFF3A2A2A;
            context.fill(controlX, controlY, controlX + btnSize, controlY + SETTING_HEIGHT, minusBg);
            drawBorder(context, controlX, controlY, btnSize, SETTING_HEIGHT, 0xFFAA6644);
            context.drawTextWithShadow(this.textRenderer, "-", controlX + btnSize / 2 - 2, controlY + 8, TEXT_PRIMARY);
            
            settingControls.add(new SettingControl(setting, module, controlX, controlY,
                btnSize, SETTING_HEIGHT, SettingControlType.NUMBER_MINUS));
            
            // Number display
            int numX = controlX + btnSize + 2;
            context.fill(numX, controlY, numX + numWidth, controlY + SETTING_HEIGHT, 0xFF1A1A2A);
            String numText = String.valueOf(value);
            int numTextWidth = this.textRenderer.getWidth(numText);
            context.drawTextWithShadow(this.textRenderer, numText,
                numX + (numWidth - numTextWidth) / 2, controlY + 8, 0xFFFFCC44);
            
            // Plus button
            int plusX = numX + numWidth + 2;
            boolean plusHovered = mouseX >= plusX && mouseX < plusX + btnSize &&
                                  mouseY >= controlY && mouseY < controlY + SETTING_HEIGHT;
            int plusBg = plusHovered ? 0xFF3A4A3A : 0xFF2A3A2A;
            context.fill(plusX, controlY, plusX + btnSize, controlY + SETTING_HEIGHT, plusBg);
            drawBorder(context, plusX, controlY, btnSize, SETTING_HEIGHT, 0xFF66AA44);
            context.drawTextWithShadow(this.textRenderer, "+", plusX + btnSize / 2 - 2, controlY + 8, TEXT_PRIMARY);
            
            settingControls.add(new SettingControl(setting, module, plusX, controlY,
                btnSize, SETTING_HEIGHT, SettingControlType.NUMBER_PLUS));
                
        } else if (setting instanceof ModuleSetting.ColorSetting colorSetting) {
            // Color preview with click to edit - full width
            int color = colorSetting.getValue();
            boolean hovered = mouseX >= controlX && mouseX < controlX + controlWidth &&
                             mouseY >= controlY && mouseY < controlY + SETTING_HEIGHT;
            
            int bg = hovered ? 0xFF3A3A4A : 0xFF2A2A3A;
            context.fill(controlX, controlY, controlX + controlWidth, controlY + SETTING_HEIGHT, bg);
            drawBorder(context, controlX, controlY, controlWidth, SETTING_HEIGHT, 0xFF888888);
            
            // Color preview box
            int previewX = controlX + 4;
            int previewY = controlY + (SETTING_HEIGHT - COLOR_PREVIEW_SIZE) / 2;
            context.fill(previewX, previewY, previewX + COLOR_PREVIEW_SIZE, previewY + COLOR_PREVIEW_SIZE, color | 0xFF000000);
            drawBorder(context, previewX, previewY, COLOR_PREVIEW_SIZE, COLOR_PREVIEW_SIZE, 0xFFFFFFFF);
            
            // Hex value
            String hexText = String.format("#%06X", color & 0xFFFFFF);
            context.drawTextWithShadow(this.textRenderer, hexText,
                previewX + COLOR_PREVIEW_SIZE + 4, controlY + 8, TEXT_SECONDARY);
            
            settingControls.add(new SettingControl(setting, module, controlX, controlY,
                controlWidth, SETTING_HEIGHT, SettingControlType.COLOR));
        }
        
        // Return y + label height + control height + spacing
        return y + 12 + SETTING_HEIGHT + 6;
    }
    
    /**
     * Draws a 1-pixel border around a rectangle.
     */
    private void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);  // Top
        context.fill(x, y + height - 1, x + width, y + height, color);  // Bottom
        context.fill(x, y, x + 1, y + height, color);  // Left
        context.fill(x + width - 1, y, x + width, y + height, color);  // Right
    }
    
    /**
     * Handles clicks on setting controls.
     */
    private boolean handleSettingClick(double mouseX, double mouseY) {
        for (SettingControl control : settingControls) {
            if (mouseX >= control.x && mouseX < control.x + control.width &&
                mouseY >= control.y && mouseY < control.y + control.height) {
                
                switch (control.type) {
                    case BOOLEAN -> {
                        ModuleSetting.BooleanSetting boolSetting = (ModuleSetting.BooleanSetting) control.setting;
                        boolSetting.setValue(!boolSetting.getValue());
                        ModuleManager.getInstance().saveConfig();
                        return true;
                    }
                    case ENUM -> {
                        cycleEnumSetting(control.setting);
                        ModuleManager.getInstance().saveConfig();
                        return true;
                    }
                    case NUMBER_MINUS -> {
                        ModuleSetting.NumberSetting numSetting = (ModuleSetting.NumberSetting) control.setting;
                        int newValue = Math.max(numSetting.getMin(), numSetting.getValue() - 1);
                        numSetting.setValue(newValue);
                        ModuleManager.getInstance().saveConfig();
                        return true;
                    }
                    case NUMBER_PLUS -> {
                        ModuleSetting.NumberSetting numSetting = (ModuleSetting.NumberSetting) control.setting;
                        int newValue = Math.min(numSetting.getMax(), numSetting.getValue() + 1);
                        numSetting.setValue(newValue);
                        ModuleManager.getInstance().saveConfig();
                        return true;
                    }
                    case COLOR -> {
                        // Open color picker (simplified - cycle through preset colors)
                        cycleColorSetting((ModuleSetting.ColorSetting) control.setting);
                        ModuleManager.getInstance().saveConfig();
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Cycles through enum values.
     */
    @SuppressWarnings("unchecked")
    private <E extends Enum<E>> void cycleEnumSetting(ModuleSetting<?> setting) {
        if (setting instanceof ModuleSetting.EnumSetting<?> enumSetting) {
            E[] values = (E[]) ((ModuleSetting.EnumSetting<E>) enumSetting).getOptions();
            E current = (E) enumSetting.getValue();
            int currentIndex = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == current) {
                    currentIndex = i;
                    break;
                }
            }
            int nextIndex = (currentIndex + 1) % values.length;
            ((ModuleSetting.EnumSetting<E>) enumSetting).setValue(values[nextIndex]);
        }
    }
    
    /**
     * Cycles through preset colors for color settings.
     */
    private void cycleColorSetting(ModuleSetting.ColorSetting setting) {
        int[] presetColors = {
            0xFFFF5555,  // Red
            0xFFFF5500,  // Orange
            0xFFFFAA00,  // Gold
            0xFFFFFF55,  // Yellow
            0xFF55FF55,  // Green
            0xFF55FFFF,  // Cyan
            0xFF5555FF,  // Blue
            0xFFAA55FF,  // Purple
            0xFFFF55FF,  // Pink
            0xFFFFFFFF,  // White
        };
        
        int current = setting.getValue() | 0xFF000000;
        int nextIndex = 0;
        for (int i = 0; i < presetColors.length; i++) {
            if (presetColors[i] == current) {
                nextIndex = (i + 1) % presetColors.length;
                break;
            }
        }
        setting.setValue(presetColors[nextIndex]);
    }
    
    /**
     * Record for tracking clickable setting controls.
     */
    private record SettingControl(
        ModuleSetting<?> setting,
        AlertModule module,
        int x, int y, int width, int height,
        SettingControlType type
    ) {}
    
    private enum SettingControlType {
        BOOLEAN, ENUM, NUMBER_MINUS, NUMBER_PLUS, COLOR
    }
}
