package net.shlomo1412.booster.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.Module;
import net.shlomo1412.booster.client.module.ModuleManager;

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
    private static final int SIDEBAR_WIDTH = 180;
    private static final int CONTENT_PADDING = 16;
    private static final int MODULE_CARD_HEIGHT = 80;
    private static final int MODULE_CARD_SPACING = 8;
    
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
    
    // Tabs/Categories
    private final Map<String, List<Module>> categories = new LinkedHashMap<>();
    private String activeCategory = "All";
    
    // Scrolling
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private float maxScrollOffset = 0f;
    private static final float SCROLL_SPEED = 0.25f;
    private static final int SCROLL_AMOUNT = 30;
    
    // Selected module for details panel
    private Module selectedModule = null;
    
    // Animation
    private float openAnimation = 0f;
    
    // Parent screen to return to
    private final Screen parent;
    
    public BoosterConfigScreen(Screen parent) {
        super(Text.literal("Booster Configuration"));
        this.parent = parent;
        initializeCategories();
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
        List<Module> modules = categories.get(activeCategory);
        if (modules == null || modules.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, "No modules in this category",
                x + width / 2, y + height / 2, TEXT_DIM);
            return;
        }
        
        // Calculate total content height and max scroll
        int totalHeight = modules.size() * (MODULE_CARD_HEIGHT + MODULE_CARD_SPACING);
        maxScrollOffset = Math.max(0, totalHeight - height + CONTENT_PADDING * 2);
        
        // Scissor for clipping
        context.enableScissor(x, y, x + width, y + height);
        
        int cardY = y + CONTENT_PADDING - (int) scrollOffset;
        
        for (Module module : modules) {
            if (cardY + MODULE_CARD_HEIGHT >= y && cardY < y + height) {
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
                context.fill(x, y + i, x + width, y + i + 1, (alpha << 24) | 0x0D0D0D);
            }
        }
        if (scrollOffset < maxScrollOffset) {
            // Bottom fade
            for (int i = 0; i < 20; i++) {
                int alpha = (int) ((1 - i / 20f) * 200);
                context.fill(x, y + height - i - 1, x + width, y + height - i, (alpha << 24) | 0x0D0D0D);
            }
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
            return;
        }
        
        int contentX = x + CONTENT_PADDING;
        int contentY = y + CONTENT_PADDING;
        int contentWidth = width - CONTENT_PADDING * 2;
        
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
        }
        
        // Type
        String type = selectedModule instanceof GUIModule ? "GUI Module" : "Utility Module";
        int typeColor = selectedModule instanceof GUIModule ? 0xFF5588FF : 0xFFAA55FF;
        context.drawTextWithShadow(this.textRenderer, "Type: " + type, contentX, contentY, typeColor);
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
        // Tab clicks
        int tabY = HEADER_HEIGHT;
        int tabX = 0;
        for (String category : categories.keySet()) {
            if (mouseX >= tabX && mouseX < tabX + TAB_WIDTH &&
                mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
                activeCategory = category;
                scrollOffset = 0;
                targetScrollOffset = 0;
                return true;
            }
            tabX += TAB_WIDTH;
        }
        
        // Module card clicks
        int contentY = HEADER_HEIGHT + TAB_HEIGHT;
        int contentHeight = this.height - contentY;
        int listWidth = this.width - SIDEBAR_WIDTH - CONTENT_PADDING * 2;
        
        List<Module> modules = categories.get(activeCategory);
        if (modules != null) {
            int cardY = contentY + CONTENT_PADDING - (int) scrollOffset;
            
            for (Module module : modules) {
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
                    return true;
                }
                
                cardY += MODULE_CARD_HEIGHT + MODULE_CARD_SPACING;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        targetScrollOffset -= verticalAmount * SCROLL_AMOUNT;
        targetScrollOffset = MathHelper.clamp(targetScrollOffset, 0, maxScrollOffset);
        return true;
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
}
