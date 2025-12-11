package net.shlomo1412.booster.client.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.shlomo1412.booster.client.editor.DraggableWidget;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.modules.SortMode;

import java.util.function.Consumer;

/**
 * A button widget specifically for sorting functionality.
 * Supports Alt+Scroll to change sort mode.
 * Supports display modes: Icon Only, Name + Icon, Automatic.
 */
public class SortButton extends ButtonWidget implements DraggableWidget {
    
    private final String baseIcon;
    private final String actionDescription;
    private SortMode currentMode;
    private final Consumer<SortMode> onSort;
    private final Runnable onModeChanged;
    
    // Display mode
    private ButtonDisplayMode displayMode = ButtonDisplayMode.AUTO;
    
    // Editor mode support
    private GUIModule parentModule;
    private String widgetId;
    private String displayName;
    private int anchorX;
    private int anchorY;
    
    // Tooltip texts
    private Tooltip normalTooltip;
    private Tooltip extendedTooltip;
    
    /**
     * Creates a new sort button.
     *
     * @param x X position
     * @param y Y position
     * @param width Button width
     * @param height Button height
     * @param icon The icon to display
     * @param displayName Display name for the button
     * @param initialMode Initial sort mode
     * @param onSort Action to perform when sorting (receives current mode)
     * @param onModeChanged Called when mode changes (for saving config)
     */
    public SortButton(int x, int y, int width, int height, String icon, 
                      String displayName, SortMode initialMode,
                      Consumer<SortMode> onSort, Runnable onModeChanged) {
        super(x, y, width, height, Text.literal(icon), 
              button -> onSort.accept(initialMode), DEFAULT_NARRATION_SUPPLIER);
        this.baseIcon = icon;
        this.actionDescription = displayName;
        this.displayName = displayName;
        this.currentMode = initialMode;
        this.onSort = onSort;
        this.onModeChanged = onModeChanged;
        updateTooltips();
        updateDisplayedText();
    }
    
    /**
     * Sets the display mode for this button.
     */
    public void setDisplayMode(ButtonDisplayMode mode) {
        this.displayMode = mode;
        updateDisplayedText();
    }
    
    /**
     * @return The current display mode
     */
    public ButtonDisplayMode getDisplayMode() {
        return displayMode;
    }
    
    /**
     * Updates the displayed text based on the display mode and button size.
     */
    public void updateDisplayedText() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.textRenderer == null) {
            setMessage(Text.literal(baseIcon));
            return;
        }
        
        boolean showName = switch (displayMode) {
            case ICON_ONLY -> false;
            case NAME_AND_ICON -> true;
            case AUTO -> {
                // Auto: show name if button is wide enough
                int nameWidth = client.textRenderer.getWidth(actionDescription);
                int minWidth = ButtonDisplayMode.getMinWidthForNameAndIcon(nameWidth);
                yield width >= minWidth;
            }
        };
        
        if (showName) {
            setMessage(Text.literal(baseIcon + " " + actionDescription));
        } else {
            setMessage(Text.literal(baseIcon));
        }
    }
    
    /**
     * Updates tooltip text based on current mode.
     */
    private void updateTooltips() {
        this.normalTooltip = Tooltip.of(createNormalTooltip());
        this.extendedTooltip = Tooltip.of(createExtendedTooltip());
        setTooltip(normalTooltip);
    }
    
    private Text createNormalTooltip() {
        MutableText text = Text.empty();
        
        // Button name in gold
        text.append(Text.literal(displayName).formatted(Formatting.GOLD));
        text.append(Text.literal("\n"));
        
        // Current mode
        text.append(Text.literal("Mode: ").formatted(Formatting.GRAY));
        text.append(Text.literal(currentMode.getDisplayName()).formatted(Formatting.YELLOW));
        text.append(Text.literal("\n\n"));
        
        // Hint
        text.append(Text.literal("Hold ").formatted(Formatting.DARK_GRAY));
        text.append(Text.literal("ALT").formatted(Formatting.AQUA, Formatting.BOLD));
        text.append(Text.literal(" + ").formatted(Formatting.DARK_GRAY));
        text.append(Text.literal("Scroll").formatted(Formatting.AQUA, Formatting.BOLD));
        text.append(Text.literal(" to change mode").formatted(Formatting.DARK_GRAY));
        
        return text;
    }
    
    private Text createExtendedTooltip() {
        MutableText text = Text.empty();
        
        // Button name
        text.append(Text.literal(displayName).formatted(Formatting.GOLD, Formatting.BOLD));
        text.append(Text.literal("\n\n"));
        
        // Current mode with description
        text.append(Text.literal("Current Mode: ").formatted(Formatting.GRAY));
        text.append(Text.literal(currentMode.getDisplayName()).formatted(Formatting.YELLOW, Formatting.BOLD));
        text.append(Text.literal("\n"));
        text.append(Text.literal(currentMode.getDescription()).formatted(Formatting.GRAY, Formatting.ITALIC));
        text.append(Text.literal("\n\n"));
        
        // All modes list
        text.append(Text.literal("Available Modes:").formatted(Formatting.WHITE));
        for (SortMode mode : SortMode.values()) {
            text.append(Text.literal("\n"));
            boolean isCurrent = mode == currentMode;
            text.append(Text.literal(isCurrent ? "▶ " : "  ").formatted(isCurrent ? Formatting.YELLOW : Formatting.DARK_GRAY));
            text.append(Text.literal(mode.getDisplayName()).formatted(isCurrent ? Formatting.YELLOW : Formatting.GRAY));
        }
        
        return text;
    }
    
    /**
     * Gets the current sort mode.
     */
    public SortMode getCurrentMode() {
        return currentMode;
    }
    
    /**
     * Sets the sort mode.
     */
    public void setMode(SortMode mode) {
        this.currentMode = mode;
        updateTooltips();
    }
    
    /**
     * Cycles to the next sort mode.
     */
    public void nextMode() {
        currentMode = currentMode.next();
        updateTooltips();
        if (onModeChanged != null) {
            onModeChanged.run();
        }
    }
    
    /**
     * Cycles to the previous sort mode.
     */
    public void previousMode() {
        currentMode = currentMode.previous();
        updateTooltips();
        if (onModeChanged != null) {
            onModeChanged.run();
        }
    }
    
    /**
     * Handles mouse scroll for changing sort mode.
     * Returns true if the scroll was handled.
     */
    public boolean handleScroll(double mouseX, double mouseY, double amount) {
        if (isMouseOver(mouseX, mouseY) && Screen.hasAltDown()) {
            if (amount > 0) {
                previousMode();
            } else if (amount < 0) {
                nextMode();
            }
            return true;
        }
        return false;
    }
    
    @Override
    public void onClick(double mouseX, double mouseY) {
        // Perform the sort with current mode
        onSort.accept(currentMode);
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Update tooltip based on CTRL key state
        if (Screen.hasControlDown()) {
            setTooltip(extendedTooltip);
        } else {
            setTooltip(normalTooltip);
        }
        
        // Draw editor mode highlight if selected
        EditorModeManager editor = EditorModeManager.getInstance();
        if (editor.isEditorModeActive()) {
            boolean isSelected = editor.getSelectedWidget() == this;
            boolean isHoveredInEditor = isMouseOver(mouseX, mouseY);
            DraggableWidget.ResizeEdge hoverEdge = getResizeEdge(mouseX, mouseY);

            // Draw highlight border
            int borderColor = isSelected ? 0xFFFFAA00 : (isHoveredInEditor ? 0xFF888888 : 0xFF444444);
            context.fill(getX() - 1, getY() - 1, getX() + width + 1, getY(), borderColor);
            context.fill(getX() - 1, getY() + height, getX() + width + 1, getY() + height + 1, borderColor);
            context.fill(getX() - 1, getY(), getX(), getY() + height, borderColor);
            context.fill(getX() + width, getY(), getX() + width + 1, getY() + height, borderColor);

            // Draw selection border and resize handles when selected
            if (isSelected) {
                // Outer selection border
                context.fill(getX() - 2, getY() - 2, getX() + width + 2, getY() - 1, 0xFFFFAA00);
                context.fill(getX() - 2, getY() + height + 1, getX() + width + 2, getY() + height + 2, 0xFFFFAA00);
                context.fill(getX() - 2, getY() - 1, getX() - 1, getY() + height + 1, 0xFFFFAA00);
                context.fill(getX() + width + 1, getY() - 1, getX() + width + 2, getY() + height + 1, 0xFFFFAA00);

                // Draw resize handles
                int handleSize = RESIZE_HANDLE_SIZE;
                int handleColor = 0xFFFFFFFF;
                int handleBorder = 0xFFFFAA00;

                drawResizeHandle(context, getX() - handleSize/2, getY() - handleSize/2, handleSize, handleColor, handleBorder);
                drawResizeHandle(context, getX() + width - handleSize/2, getY() - handleSize/2, handleSize, handleColor, handleBorder);
                drawResizeHandle(context, getX() - handleSize/2, getY() + height - handleSize/2, handleSize, handleColor, handleBorder);
                drawResizeHandle(context, getX() + width - handleSize/2, getY() + height - handleSize/2, handleSize, handleColor, handleBorder);
            }

            // Highlight resize edge on hover
            if (hoverEdge != null && !isSelected) {
                int edgeColor = 0x80FFAA00;
                switch (hoverEdge) {
                    case TOP, TOP_LEFT, TOP_RIGHT -> context.fill(getX() - 1, getY() - 2, getX() + width + 1, getY(), edgeColor);
                    case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> context.fill(getX() - 1, getY() + height, getX() + width + 1, getY() + height + 2, edgeColor);
                }
                switch (hoverEdge) {
                    case LEFT, TOP_LEFT, BOTTOM_LEFT -> context.fill(getX() - 2, getY() - 1, getX(), getY() + height + 1, edgeColor);
                    case RIGHT, TOP_RIGHT, BOTTOM_RIGHT -> context.fill(getX() + width, getY() - 1, getX() + width + 2, getY() + height + 1, edgeColor);
                }
            }
        }
        
        super.renderWidget(context, mouseX, mouseY, delta);
        
        // Draw mode indicator (small dot or letter)
        if (!EditorModeManager.getInstance().isEditorModeActive()) {
            String modeIndicator = currentMode.getDisplayName().substring(0, 1);
            int indicatorX = getX() + width - 6;
            int indicatorY = getY() + height - 8;
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, 
                modeIndicator, indicatorX, indicatorY, 0xFFFFAA00);
        }
    }
    
    private void drawResizeHandle(DrawContext context, int x, int y, int size, int fillColor, int borderColor) {
        context.fill(x, y, x + size, y + size, borderColor);
        context.fill(x + 1, y + 1, x + size - 1, y + size - 1, fillColor);
    }
    
    // ==================== DraggableWidget Implementation ====================
    
    public void setEditorInfo(GUIModule module, String widgetId, String displayName, int anchorX, int anchorY) {
        this.parentModule = module;
        this.widgetId = widgetId;
        this.displayName = displayName;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        updateTooltips();
        EditorModeManager.getInstance().registerDraggableWidget(this);
    }
    
    public String getWidgetId() {
        return widgetId;
    }

    @Override
    public void setEditorPosition(int x, int y) {
        setX(x);
        setY(y);
    }

    @Override
    public void setEditorSize(int width, int height) {
        this.width = width;
        this.height = height;
        updateDisplayedText();
    }

    @Override
    public void savePosition() {
        if (parentModule != null && widgetId != null) {
            int newOffsetX = getX() - anchorX;
            int newOffsetY = getY() - anchorY;
            parentModule.updateWidgetOffset(widgetId, newOffsetX, newOffsetY);
            parentModule.updateWidgetSize(widgetId, width, height);
        }
    }

    @Override
    public GUIModule getModule() {
        return parentModule;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public void updateAnchor(int anchorX, int anchorY) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }
}
