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

/**
 * Base button widget for Booster with enhanced tooltip support.
 * Shows action description normally, full description when CTRL is held.
 * Implements DraggableWidget for editor mode support.
 * Supports different display modes: Icon Only, Name + Icon, Automatic.
 */
public class BoosterButton extends ButtonWidget implements DraggableWidget {
    private final String icon;
    private final String actionDescription;
    private final String fullDescription;
    private final Tooltip normalTooltip;
    private final Tooltip extendedTooltip;
    
    // Display mode
    private ButtonDisplayMode displayMode = ButtonDisplayMode.AUTO;
    
    // Editor mode support
    private GUIModule parentModule;
    private String widgetId;     // Unique ID for per-widget settings
    private String displayName;
    private int anchorX; // The anchor X position used for offset calculation
    private int anchorY; // The anchor Y position used for offset calculation

    /**
     * Creates a new Booster button.
     *
     * @param x                 X position
     * @param y                 Y position
     * @param width             Button width
     * @param height            Button height
     * @param icon              The icon/text to display on the button (emoji/unicode)
     * @param actionDescription Short description of what the button does
     * @param fullDescription   Full description shown when CTRL is held
     * @param onPress           Action to perform when pressed
     */
    public BoosterButton(int x, int y, int width, int height, String icon,
                         String actionDescription, String fullDescription,
                         PressAction onPress) {
        super(x, y, width, height, Text.literal(icon), onPress, DEFAULT_NARRATION_SUPPLIER);
        this.icon = icon;
        this.actionDescription = actionDescription;
        this.fullDescription = fullDescription;
        this.displayName = actionDescription;
        this.normalTooltip = Tooltip.of(createNormalTooltip());
        this.extendedTooltip = Tooltip.of(createExtendedTooltip());
        setTooltip(normalTooltip);
        updateDisplayedText();
    }
    
    /**
     * Sets the display mode for this button.
     * @param mode The display mode to use
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
            setMessage(Text.literal(icon));
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
            setMessage(Text.literal(icon + " " + actionDescription));
        } else {
            setMessage(Text.literal(icon));
        }
    }
    
    /**
     * @return The icon string
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Creates the normal tooltip with consistent formatting.
     */
    private Text createNormalTooltip() {
        MutableText text = Text.empty();
        
        // Action name in gold
        text.append(Text.literal(actionDescription).formatted(Formatting.GOLD));
        text.append(Text.literal("\n"));
        
        // Hint to hold CTRL in dark gray
        text.append(Text.literal("Hold ").formatted(Formatting.DARK_GRAY));
        text.append(Text.literal("CTRL").formatted(Formatting.GRAY, Formatting.BOLD));
        text.append(Text.literal(" for description").formatted(Formatting.DARK_GRAY));
        
        return text;
    }

    /**
     * Creates the extended tooltip with fancy formatting.
     */
    private Text createExtendedTooltip() {
        MutableText text = Text.empty();
        
        // Action title in gold
        text.append(Text.literal(actionDescription).formatted(Formatting.GOLD, Formatting.BOLD));
        text.append(Text.literal("\n\n"));
        
        // Full description in gray/italic
        text.append(Text.literal(fullDescription).formatted(Formatting.GRAY, Formatting.ITALIC));
        
        return text;
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

                // Draw resize handles (corners)
                int handleSize = RESIZE_HANDLE_SIZE;
                int handleColor = 0xFFFFFFFF;
                int handleBorder = 0xFFFFAA00;

                // Top-left corner
                drawResizeHandle(context, getX() - handleSize/2, getY() - handleSize/2, handleSize, handleColor, handleBorder);
                // Top-right corner
                drawResizeHandle(context, getX() + width - handleSize/2, getY() - handleSize/2, handleSize, handleColor, handleBorder);
                // Bottom-left corner
                drawResizeHandle(context, getX() - handleSize/2, getY() + height - handleSize/2, handleSize, handleColor, handleBorder);
                // Bottom-right corner
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
    }

    /**
     * Draws a resize handle square.
     */
    private void drawResizeHandle(DrawContext context, int x, int y, int size, int fillColor, int borderColor) {
        context.fill(x, y, x + size, y + size, borderColor);
        context.fill(x + 1, y + 1, x + size - 1, y + size - 1, fillColor);
    }

    // ==================== DraggableWidget Implementation ====================

    /**
     * Sets the parent module and anchor position for this button.
     * Required for editor mode drag-and-drop functionality.
     * 
     * @param module The parent module
     * @param widgetId Unique ID for this widget's settings
     * @param displayName Display name shown in editor
     * @param anchorX Anchor X position for offset calculation
     * @param anchorY Anchor Y position for offset calculation
     */
    public void setEditorInfo(GUIModule module, String widgetId, String displayName, int anchorX, int anchorY) {
        this.parentModule = module;
        this.widgetId = widgetId;
        this.displayName = displayName;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        
        // Register with editor manager
        EditorModeManager.getInstance().registerDraggableWidget(this);
    }
    
    /**
     * @return The widget ID for this button
     */
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
            // Calculate new offset relative to anchor
            int newOffsetX = getX() - anchorX;
            int newOffsetY = getY() - anchorY;
            
            // Update per-widget settings
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

    /**
     * Updates the anchor position (call when screen is resized).
     */
    public void updateAnchor(int anchorX, int anchorY) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    /**
     * Builder class for creating BoosterButtons with a fluent API.
     */
    public static class Builder {
        private int x, y;
        private int width = 20;
        private int height = 20;
        private String icon = "?";
        private String actionDescription = "";
        private String fullDescription = "";
        private PressAction onPress = button -> {};

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder actionDescription(String description) {
            this.actionDescription = description;
            return this;
        }

        public Builder fullDescription(String description) {
            this.fullDescription = description;
            return this;
        }

        public Builder onPress(PressAction action) {
            this.onPress = action;
            return this;
        }

        public BoosterButton build() {
            return new BoosterButton(x, y, width, height, icon, 
                    actionDescription, fullDescription, onPress);
        }
    }

    /**
     * @return A new builder for creating a BoosterButton
     */
    public static Builder builder() {
        return new Builder();
    }
}
