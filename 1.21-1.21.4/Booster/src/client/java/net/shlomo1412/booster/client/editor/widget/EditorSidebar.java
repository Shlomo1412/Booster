package net.shlomo1412.booster.client.editor.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.shlomo1412.booster.client.editor.DraggableWidget;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.editor.ScreenInfo;
import net.shlomo1412.booster.client.module.GUIModule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Sidebar panel for the Booster editor mode.
 * Shows screen info, dev details, and module controls.
 * Collapsible with smooth animations.
 */
public class EditorSidebar implements Drawable, Element {
    private static final int SIDEBAR_WIDTH = 200;
    private static final int COLLAPSED_WIDTH = 24;
    private static final int PADDING = 10;
    private static final int HEADER_HEIGHT = 32;
    private static final int SECTION_SPACING = 8;
    private static final int LINE_HEIGHT = 12;
    private static final int FOOTER_HEIGHT = 50;

    // Animation
    private static final float ANIMATION_SPEED = 0.15f;
    private float animationProgress = 0f; // 0 = hidden, 1 = fully visible
    private boolean isOpening = true;
    
    // Collapse state
    private boolean isCollapsed = false;
    private float collapseProgress = 0f; // 0 = expanded, 1 = collapsed

    // Scrolling
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private float maxScrollOffset = 0f;
    private static final float SCROLL_SPEED = 0.3f;
    private static final int SCROLL_AMOUNT = 20;

    // Colors
    private static final int BG_COLOR = 0xF0181818;
    private static final int HEADER_BG_COLOR = 0xFF1a1a1a;
    private static final int BORDER_COLOR = 0xFF3a3a3a;
    private static final int ACCENT_COLOR = 0xFFFFAA00;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_DIM_COLOR = 0xFFAAAAAA;
    private static final int SECTION_BG_COLOR = 0xFF222222;
    private static final int SECTION_HOVER_COLOR = 0xFF2a2a2a;
    private static final int COLLAPSE_BTN_COLOR = 0xFF333333;
    private static final int COLLAPSE_BTN_HOVER = 0xFF444444;

    private final MinecraftClient client;
    private final TextRenderer textRenderer;
    private final ScreenInfo screenInfo;
    private final List<GUIModule> activeModules;

    private int screenWidth, screenHeight;
    private int height;
    private boolean devDetailsExpanded = false;
    private final Set<String> expandedModules = new HashSet<>();

    private boolean focused = false;
    private int hoveredModuleIndex = -1;
    private boolean collapseButtonHovered = false;

    public EditorSidebar(MinecraftClient client, ScreenInfo screenInfo,
                         List<GUIModule> activeModules, Consumer<Element> addChild) {
        this.client = client;
        this.textRenderer = client.textRenderer;
        this.screenInfo = screenInfo;
        this.activeModules = activeModules;

        this.screenWidth = client.getWindow().getScaledWidth();
        this.screenHeight = client.getWindow().getScaledHeight();
        this.height = screenHeight;

        // Start animation
        this.animationProgress = 0f;
        this.isOpening = true;
    }

    /**
     * Updates the animation state. Call this every frame.
     */
    public void tick() {
        // Smooth open/close animation
        if (isOpening) {
            animationProgress = MathHelper.lerp(ANIMATION_SPEED, animationProgress, 1f);
            if (animationProgress > 0.99f) animationProgress = 1f;
        } else {
            animationProgress = MathHelper.lerp(ANIMATION_SPEED, animationProgress, 0f);
            if (animationProgress < 0.01f) animationProgress = 0f;
        }
        
        // Smooth collapse animation
        float targetCollapse = isCollapsed ? 1f : 0f;
        collapseProgress = MathHelper.lerp(ANIMATION_SPEED, collapseProgress, targetCollapse);
        if (Math.abs(collapseProgress - targetCollapse) < 0.01f) {
            collapseProgress = targetCollapse;
        }

        // Smooth scrolling
        scrollOffset = MathHelper.lerp(SCROLL_SPEED, scrollOffset, targetScrollOffset);
    }

    /**
     * Gets the current width based on collapse state.
     */
    private int getCurrentWidth() {
        return (int) MathHelper.lerp(collapseProgress, SIDEBAR_WIDTH, COLLAPSED_WIDTH);
    }

    /**
     * Starts the close animation.
     */
    public void close() {
        isOpening = false;
    }

    /**
     * @return true if the sidebar is fully closed and can be removed
     */
    public boolean isClosed() {
        return !isOpening && animationProgress <= 0.01f;
    }

    public void updatePosition(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.height = screenHeight;
    }

    /**
     * Gets the current X position with animation applied.
     */
    private int getAnimatedX() {
        int currentWidth = getCurrentWidth();
        // Slide in from left: when progress=0, x=-width; when progress=1, x=0
        return (int) (-currentWidth * (1f - animationProgress));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update animation
        tick();

        int currentWidth = getCurrentWidth();
        int x = getAnimatedX();
        int y = 0;

        // Check collapse button hover
        int collapseBtnX = x + currentWidth - 20;
        int collapseBtnY = y + 6;
        collapseButtonHovered = mouseX >= collapseBtnX && mouseX < collapseBtnX + 16 &&
                               mouseY >= collapseBtnY && mouseY < collapseBtnY + 20;

        // Use scissor to clip content that overflows
        context.enableScissor(Math.max(0, x), y, x + currentWidth, y + height);

        // Main background
        context.fill(x, y, x + currentWidth, y + height, BG_COLOR);

        // Right border (accent line)
        context.fill(x + currentWidth - 2, y, x + currentWidth, y + height, ACCENT_COLOR);
        context.fill(x + currentWidth - 1, y, x + currentWidth, y + height, BORDER_COLOR);

        if (isCollapsed || collapseProgress > 0.5f) {
            // Render collapsed view
            renderCollapsed(context, x, y, currentWidth, mouseX, mouseY);
        } else {
            // Render expanded view
            renderExpanded(context, x, y, currentWidth, mouseX, mouseY);
        }

        context.disableScissor();
    }

    /**
     * Renders the collapsed sidebar (just a button bar).
     */
    private void renderCollapsed(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
        // Header with expand button
        context.fill(x, y, x + width, y + HEADER_HEIGHT, HEADER_BG_COLOR);
        
        // Expand button (>)
        int btnX = x + 4;
        int btnY = y + 6;
        int btnColor = collapseButtonHovered ? COLLAPSE_BTN_HOVER : COLLAPSE_BTN_COLOR;
        context.fill(btnX, btnY, btnX + 16, btnY + 20, btnColor);
        context.fill(btnX, btnY, btnX + 16, btnY + 1, ACCENT_COLOR);
        context.fill(btnX, btnY + 19, btnX + 16, btnY + 20, ACCENT_COLOR);
        context.fill(btnX, btnY, btnX + 1, btnY + 20, ACCENT_COLOR);
        context.fill(btnX + 15, btnY, btnX + 16, btnY + 20, ACCENT_COLOR);
        
        // Arrow icon
        context.drawCenteredTextWithShadow(textRenderer, "▶", btnX + 8, btnY + 6, ACCENT_COLOR);
    }

    /**
     * Renders the expanded sidebar content.
     */
    private void renderExpanded(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
        // === HEADER ===
        context.fill(x, y, x + width, y + HEADER_HEIGHT, HEADER_BG_COLOR);
        context.fill(x, y + HEADER_HEIGHT - 1, x + width - 2, y + HEADER_HEIGHT, BORDER_COLOR);

        // Title with icon
        String title = "✏ Editor Mode";
        context.drawTextWithShadow(textRenderer, title,
                x + PADDING, y + (HEADER_HEIGHT - 8) / 2, ACCENT_COLOR);

        // Collapse button (<)
        int btnX = x + width - 22;
        int btnY = y + 6;
        int btnColor = collapseButtonHovered ? COLLAPSE_BTN_HOVER : COLLAPSE_BTN_COLOR;
        context.fill(btnX, btnY, btnX + 18, btnY + 20, btnColor);
        context.fill(btnX, btnY, btnX + 18, btnY + 1, ACCENT_COLOR);
        context.fill(btnX, btnY + 19, btnX + 18, btnY + 20, ACCENT_COLOR);
        context.fill(btnX, btnY, btnX + 1, btnY + 20, ACCENT_COLOR);
        context.fill(btnX + 17, btnY, btnX + 18, btnY + 20, ACCENT_COLOR);
        context.drawCenteredTextWithShadow(textRenderer, "◀", btnX + 9, btnY + 6, ACCENT_COLOR);

        // === SCROLLABLE CONTENT ===
        int contentStartY = y + HEADER_HEIGHT;
        int contentEndY = y + height - FOOTER_HEIGHT;
        int contentHeight = contentEndY - contentStartY;

        // Apply scroll offset for content
        context.enableScissor(Math.max(0, x), contentStartY, x + width - 2, contentEndY);

        int currentY = contentStartY + PADDING - (int) scrollOffset;

        // Screen title
        String screenTitle = screenInfo.getScreenTitle();
        if (screenTitle.length() > 22) {
            screenTitle = screenTitle.substring(0, 19) + "...";
        }
        context.drawTextWithShadow(textRenderer,
                Text.literal(screenTitle).formatted(Formatting.WHITE, Formatting.BOLD),
                x + PADDING, currentY, TEXT_COLOR);
        currentY += LINE_HEIGHT + SECTION_SPACING;

        // === DEV DETAILS SECTION ===
        currentY = renderDevDetailsSection(context, x, currentY, mouseX, mouseY);
        currentY += SECTION_SPACING;

        // === MODULES HEADER ===
        context.drawTextWithShadow(textRenderer,
                Text.literal("Modules").formatted(Formatting.GOLD),
                x + PADDING, currentY, ACCENT_COLOR);
        currentY += LINE_HEIGHT + 6;

        // === MODULE SECTIONS ===
        hoveredModuleIndex = -1;
        for (int i = 0; i < activeModules.size(); i++) {
            GUIModule module = activeModules.get(i);
            currentY = renderModuleSection(context, x, currentY, mouseX, mouseY, module, i);
            currentY += SECTION_SPACING;
        }

        // === SELECTED WIDGET INFO ===
        DraggableWidget selected = EditorModeManager.getInstance().getSelectedWidget();
        if (selected != null) {
            currentY += SECTION_SPACING;
            context.fill(x + PADDING, currentY, x + width - PADDING - 2, currentY + 1, ACCENT_COLOR);
            currentY += SECTION_SPACING;

            context.drawTextWithShadow(textRenderer,
                    Text.literal("Selected:").formatted(Formatting.GRAY),
                    x + PADDING, currentY, TEXT_COLOR);
            currentY += LINE_HEIGHT;

            context.drawTextWithShadow(textRenderer,
                    Text.literal("  " + selected.getDisplayName()).formatted(Formatting.WHITE),
                    x + PADDING, currentY, TEXT_COLOR);
            currentY += LINE_HEIGHT;

            context.drawTextWithShadow(textRenderer,
                    "  Pos: (" + selected.getX() + ", " + selected.getY() + ")",
                    x + PADDING, currentY, TEXT_DIM_COLOR);
            currentY += LINE_HEIGHT;
        }

        // Calculate max scroll based on content height
        int totalContentHeight = currentY + (int) scrollOffset - contentStartY + PADDING;
        maxScrollOffset = Math.max(0, totalContentHeight - contentHeight);

        context.disableScissor();

        // === SCROLL INDICATOR ===
        if (maxScrollOffset > 0) {
            renderScrollbar(context, x, contentStartY, contentHeight);
        }

        // === FOOTER ===
        int footerY = y + height - FOOTER_HEIGHT;
        context.fill(x, footerY, x + width - 2, footerY + 1, BORDER_COLOR);
        context.fill(x, footerY + 1, x + width - 2, y + height, HEADER_BG_COLOR);

        context.drawTextWithShadow(textRenderer,
                Text.literal("Drag widgets to reposition").formatted(Formatting.GRAY),
                x + PADDING, footerY + 10, TEXT_DIM_COLOR);
        context.drawTextWithShadow(textRenderer,
                Text.literal("Press ").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal("✏").formatted(Formatting.GOLD))
                        .append(Text.literal(" to exit").formatted(Formatting.DARK_GRAY)),
                x + PADDING, footerY + 10 + LINE_HEIGHT + 4, TEXT_DIM_COLOR);
    }

    private int renderDevDetailsSection(DrawContext context, int x, int startY, int mouseX, int mouseY) {
        int currentY = startY;

        // Section header
        boolean headerHovered = mouseX >= x + PADDING && mouseX < x + getCurrentWidth() - PADDING &&
                mouseY >= currentY && mouseY < currentY + 18;
        int headerBg = headerHovered ? SECTION_HOVER_COLOR : SECTION_BG_COLOR;
        context.fill(x + PADDING, currentY, x + getCurrentWidth() - PADDING - 2, currentY + 18, headerBg);

        String arrow = devDetailsExpanded ? "▼" : "▶";
        context.drawTextWithShadow(textRenderer, arrow + " Dev Details",
                x + PADDING + 4, currentY + 5, TEXT_DIM_COLOR);
        currentY += 20;

        // Expanded content
        if (devDetailsExpanded) {
            context.fill(x + PADDING, currentY, x + getCurrentWidth() - PADDING - 2,
                    currentY + getDevDetailsHeight(), SECTION_BG_COLOR);
            currentY += 4;

            for (String[] detail : screenInfo.getDevDetails()) {
                context.drawTextWithShadow(textRenderer, detail[0] + ":",
                        x + PADDING + 6, currentY, 0xFF888888);
                currentY += LINE_HEIGHT;
                context.drawTextWithShadow(textRenderer, "  " + detail[1],
                        x + PADDING + 6, currentY, TEXT_COLOR);
                currentY += LINE_HEIGHT + 2;
            }
            currentY += 4;
        }

        return currentY;
    }

    private int getDevDetailsHeight() {
        return screenInfo.getDevDetails().size() * (LINE_HEIGHT * 2 + 2) + 8;
    }

    private int renderModuleSection(DrawContext context, int x, int startY,
                                    int mouseX, int mouseY, GUIModule module, int index) {
        boolean expanded = expandedModules.contains(module.getId());
        int widgetCount = module.getWidgetIds().size();
        int sectionHeight = expanded ? (24 + LINE_HEIGHT + 2 + widgetCount * LINE_HEIGHT + 4 + (LINE_HEIGHT + 2) * 2 + 4) : 24;

        int currentWidth = getCurrentWidth();

        // Check if hovered
        boolean isHovered = mouseX >= x + PADDING && mouseX < x + currentWidth - PADDING - 2 &&
                mouseY >= startY && mouseY < startY + sectionHeight;
        if (isHovered) hoveredModuleIndex = index;

        // Section background
        int bgColor = isHovered ? SECTION_HOVER_COLOR : SECTION_BG_COLOR;
        context.fill(x + PADDING, startY, x + currentWidth - PADDING - 2, startY + sectionHeight, bgColor);

        // Status indicator (colored dot)
        int statusColor = module.isEnabled() ? 0xFF44FF44 : 0xFF666666;
        context.fill(x + PADDING + 4, startY + 8, x + PADDING + 10, startY + 14, statusColor);

        // Expand arrow and name
        String arrow = expanded ? "▼" : "▶";
        context.drawTextWithShadow(textRenderer, arrow + " " + module.getName(),
                x + PADDING + 14, startY + 6, TEXT_COLOR);

        // Toggle button
        int toggleX = x + currentWidth - PADDING - 32;
        int toggleY = startY + 4;
        String toggleText = module.isEnabled() ? "ON" : "OFF";
        int toggleBg = module.isEnabled() ? 0xFF227722 : 0xFF772222;
        int toggleBorder = module.isEnabled() ? 0xFF44AA44 : 0xFFAA4444;

        context.fill(toggleX, toggleY, toggleX + 28, toggleY + 14, toggleBg);
        context.fill(toggleX, toggleY, toggleX + 28, toggleY + 1, toggleBorder);
        context.fill(toggleX, toggleY + 13, toggleX + 28, toggleY + 14, toggleBorder);
        context.fill(toggleX, toggleY, toggleX + 1, toggleY + 14, toggleBorder);
        context.fill(toggleX + 27, toggleY, toggleX + 28, toggleY + 14, toggleBorder);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal(toggleText),
                toggleX + 14, toggleY + 3, TEXT_COLOR);

        int currentY = startY + 24;

        if (expanded) {
            // Show widget count
            Set<String> widgetIds = module.getWidgetIds();
            context.drawTextWithShadow(textRenderer, "Widgets: " + widgetIds.size(),
                    x + PADDING + 8, currentY, TEXT_DIM_COLOR);
            currentY += LINE_HEIGHT + 2;

            // Show each widget's info
            for (String widgetId : widgetIds) {
                var settings = module.getWidgetSettings(widgetId);
                String info = widgetId + ": " + settings.getOffsetX() + "," + settings.getOffsetY() +
                        " (" + settings.getWidth() + "x" + settings.getHeight() + ")";
                // Truncate if too long
                if (textRenderer.getWidth(info) > currentWidth - PADDING * 2 - 16) {
                    info = widgetId + ": " + settings.getOffsetX() + "," + settings.getOffsetY();
                }
                context.drawTextWithShadow(textRenderer, info,
                        x + PADDING + 12, currentY, TEXT_COLOR);
                currentY += LINE_HEIGHT;
            }
            currentY += 4;

            // Reset All Positions button
            boolean resetPosHovered = mouseX >= x + PADDING + 8 && mouseX < x + PADDING + 110 &&
                    mouseY >= currentY && mouseY < currentY + 14;
            int resetPosColor = resetPosHovered ? 0xFF7799FF : 0xFF5577CC;
            context.drawTextWithShadow(textRenderer, "[Reset All Positions]",
                    x + PADDING + 8, currentY, resetPosColor);
            currentY += LINE_HEIGHT + 2;

            // Reset All Sizes button
            boolean resetSizeHovered = mouseX >= x + PADDING + 8 && mouseX < x + PADDING + 100 &&
                    mouseY >= currentY && mouseY < currentY + 14;
            int resetSizeColor = resetSizeHovered ? 0xFF7799FF : 0xFF5577CC;
            context.drawTextWithShadow(textRenderer, "[Reset All Sizes]",
                    x + PADDING + 8, currentY, resetSizeColor);
            currentY += LINE_HEIGHT + 4;
        }

        return startY + sectionHeight;
    }

    private void renderScrollbar(DrawContext context, int x, int contentStartY, int contentHeight) {
        int currentWidth = getCurrentWidth();
        int scrollbarX = x + currentWidth - 6;
        int scrollbarWidth = 3;

        // Track
        context.fill(scrollbarX, contentStartY + 2, scrollbarX + scrollbarWidth,
                contentStartY + contentHeight - 2, 0xFF333333);

        // Thumb
        float thumbRatio = (float) contentHeight / (contentHeight + maxScrollOffset);
        int thumbHeight = Math.max(20, (int) (contentHeight * thumbRatio));
        float scrollRatio = scrollOffset / maxScrollOffset;
        int thumbY = contentStartY + 2 + (int) ((contentHeight - thumbHeight - 4) * scrollRatio);

        context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, ACCENT_COLOR);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        int currentWidth = getCurrentWidth();
        int x = getAnimatedX();
        int y = 0;

        // Check collapse button click
        if (isCollapsed || collapseProgress > 0.5f) {
            // Collapsed state - expand button
            int btnX = x + 4;
            int btnY = y + 6;
            if (mouseX >= btnX && mouseX < btnX + 16 && mouseY >= btnY && mouseY < btnY + 20) {
                isCollapsed = false;
                return true;
            }
        } else {
            // Expanded state - collapse button
            int btnX = x + currentWidth - 22;
            int btnY = y + 6;
            if (mouseX >= btnX && mouseX < btnX + 18 && mouseY >= btnY && mouseY < btnY + 20) {
                isCollapsed = true;
                return true;
            }
        }

        // If collapsed, don't process other clicks
        if (isCollapsed || collapseProgress > 0.5f) {
            return true;
        }

        // Content area
        int contentStartY = y + HEADER_HEIGHT;
        int currentY = contentStartY + PADDING - (int) scrollOffset;

        // Skip screen title
        currentY += LINE_HEIGHT + SECTION_SPACING;

        // Check Dev Details header click
        if (mouseY >= currentY && mouseY < currentY + 18 &&
                mouseX >= x + PADDING && mouseX < x + currentWidth - PADDING) {
            devDetailsExpanded = !devDetailsExpanded;
            return true;
        }
        currentY += 20;
        if (devDetailsExpanded) {
            currentY += getDevDetailsHeight();
        }
        currentY += SECTION_SPACING + LINE_HEIGHT + 6;

        // Check module sections
        for (GUIModule module : activeModules) {
            boolean expanded = expandedModules.contains(module.getId());
            int widgetCount = module.getWidgetIds().size();
            int sectionHeight = expanded ? (24 + LINE_HEIGHT + 2 + widgetCount * LINE_HEIGHT + 4 + (LINE_HEIGHT + 2) * 2 + 4) : 24;

            if (mouseY >= currentY && mouseY < currentY + sectionHeight) {
                // Check toggle button
                int toggleX = x + currentWidth - PADDING - 32;
                int toggleY = currentY + 4;
                if (mouseX >= toggleX && mouseX < toggleX + 28 &&
                        mouseY >= toggleY && mouseY < toggleY + 14) {
                    module.setEnabled(!module.isEnabled());
                    return true;
                }

                // Check header for expand/collapse (first 24 pixels)
                if (mouseY < currentY + 24) {
                    if (expanded) {
                        expandedModules.remove(module.getId());
                    } else {
                        expandedModules.add(module.getId());
                    }
                    return true;
                }

                // Check reset buttons if expanded
                if (expanded) {
                    int resetAreaY = currentY + 24 + LINE_HEIGHT + 2 + widgetCount * LINE_HEIGHT + 4;

                    // Reset All Positions button
                    int resetPosY = resetAreaY;
                    if (mouseY >= resetPosY && mouseY < resetPosY + 14 &&
                            mouseX >= x + PADDING + 8 && mouseX < x + PADDING + 110) {
                        module.resetAllOffsets();
                        return true;
                    }

                    // Reset All Sizes button
                    int resetSizeY = resetPosY + LINE_HEIGHT + 2;
                    if (mouseY >= resetSizeY && mouseY < resetSizeY + 14 &&
                            mouseX >= x + PADDING + 8 && mouseX < x + PADDING + 100) {
                        module.resetAllSizes();
                        return true;
                    }
                }
            }

            currentY += sectionHeight + SECTION_SPACING;
        }

        return true; // Consume click to prevent interaction with underlying screen
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseOver(mouseX, mouseY) && !isCollapsed) {
            targetScrollOffset -= verticalAmount * SCROLL_AMOUNT;
            targetScrollOffset = MathHelper.clamp(targetScrollOffset, 0, maxScrollOffset);
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        int x = getAnimatedX();
        int currentWidth = getCurrentWidth();
        return mouseX >= x && mouseX < x + currentWidth && mouseY >= 0 && mouseY < height;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    public int getWidth() {
        return getCurrentWidth();
    }

    public int getX() {
        return getAnimatedX();
    }

    /**
     * @return The right edge X position of the sidebar
     */
    public int getRightEdge() {
        return getAnimatedX() + getCurrentWidth();
    }
    
    /**
     * @return true if the sidebar is collapsed
     */
    public boolean isCollapsed() {
        return isCollapsed;
    }
}
