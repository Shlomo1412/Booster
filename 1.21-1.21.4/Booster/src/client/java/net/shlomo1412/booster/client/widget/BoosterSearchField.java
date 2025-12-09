package net.shlomo1412.booster.client.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.shlomo1412.booster.client.editor.DraggableWidget;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;

/**
 * A TextFieldWidget wrapper that implements DraggableWidget for editor mode support.
 * Also handles key events properly to prevent 'E' from closing the container.
 */
public class BoosterSearchField extends TextFieldWidget implements DraggableWidget {
    
    // Editor mode support
    private GUIModule parentModule;
    private String widgetId;
    private String displayName;
    private int anchorX;
    private int anchorY;
    
    public BoosterSearchField(int x, int y, int width, int height, Text placeholder) {
        super(MinecraftClient.getInstance().textRenderer, x, y, width, height, placeholder);
    }
    
    /**
     * Sets the editor info for drag-and-drop support.
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
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // Draw editor mode highlight if in editor mode
        EditorModeManager editor = EditorModeManager.getInstance();
        if (editor.isEditorModeActive()) {
            boolean isSelected = editor.getSelectedWidget() == this;
            boolean isHoveredInEditor = isMouseOver(mouseX, mouseY);
            DraggableWidget.ResizeEdge hoverEdge = getResizeEdge(mouseX, mouseY);

            // Draw highlight border
            int borderColor = isSelected ? 0xFFFFAA00 : (isHoveredInEditor ? 0xFF888888 : 0xFF444444);
            context.fill(getX() - 1, getY() - 1, getX() + getWidth() + 1, getY(), borderColor);
            context.fill(getX() - 1, getY() + getHeight(), getX() + getWidth() + 1, getY() + getHeight() + 1, borderColor);
            context.fill(getX() - 1, getY(), getX(), getY() + getHeight(), borderColor);
            context.fill(getX() + getWidth(), getY(), getX() + getWidth() + 1, getY() + getHeight(), borderColor);

            // Draw selection border and resize handles when selected
            if (isSelected) {
                // Outer selection border
                context.fill(getX() - 2, getY() - 2, getX() + getWidth() + 2, getY() - 1, 0xFFFFAA00);
                context.fill(getX() - 2, getY() + getHeight() + 1, getX() + getWidth() + 2, getY() + getHeight() + 2, 0xFFFFAA00);
                context.fill(getX() - 2, getY() - 1, getX() - 1, getY() + getHeight() + 1, 0xFFFFAA00);
                context.fill(getX() + getWidth() + 1, getY() - 1, getX() + getWidth() + 2, getY() + getHeight() + 1, 0xFFFFAA00);

                // Draw resize handles
                int handleSize = RESIZE_HANDLE_SIZE;
                drawResizeHandle(context, getX() - handleSize/2, getY() - handleSize/2, handleSize);
                drawResizeHandle(context, getX() + getWidth() - handleSize/2, getY() - handleSize/2, handleSize);
                drawResizeHandle(context, getX() - handleSize/2, getY() + getHeight() - handleSize/2, handleSize);
                drawResizeHandle(context, getX() + getWidth() - handleSize/2, getY() + getHeight() - handleSize/2, handleSize);
            }
        }
        
        super.renderWidget(context, mouseX, mouseY, delta);
    }
    
    private void drawResizeHandle(DrawContext context, int x, int y, int size) {
        context.fill(x, y, x + size, y + size, 0xFFFFAA00);
        context.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFFFFFFFF);
    }
    
    /**
     * Override keyPressed to consume inventory-close keys when focused.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isFocused()) {
            // Handle the key press in the text field first
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            
            // Always return true when focused to prevent the key from closing inventory
            // Exception: Escape key should still close
            if (keyCode != 256) { // 256 is ESCAPE
                return true;
            }
            return handled;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    /**
     * Override charTyped to consume all character input when focused.
     * This prevents 'E' and other keys from closing the inventory.
     */
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.isFocused()) {
            // Let the text field handle the character
            boolean handled = super.charTyped(chr, modifiers);
            // Always return true to consume the event
            return true;
        }
        return super.charTyped(chr, modifiers);
    }
    
    // ==================== DraggableWidget Implementation ====================
    
    @Override
    public void setEditorPosition(int x, int y) {
        setX(x);
        setY(y);
    }
    
    @Override
    public void setEditorSize(int width, int height) {
        setWidth(width);
        // TextFieldWidget doesn't have setHeight, so we use the constructor value
        // For now, height is fixed at creation
    }
    
    @Override
    public void savePosition() {
        if (parentModule != null && widgetId != null) {
            int offsetX = getX() - anchorX;
            int offsetY = getY() - anchorY;
            parentModule.updateWidgetOffset(widgetId, offsetX, offsetY);
            parentModule.updateWidgetSize(widgetId, getWidth(), getHeight());
        }
    }
    
    @Override
    public GUIModule getModule() {
        return parentModule;
    }
    
    @Override
    public String getDisplayName() {
        return displayName != null ? displayName : "Search Bar";
    }
    
    @Override
    public boolean isResizable() {
        return true;  // Allow horizontal resizing at least
    }
    
    public String getWidgetId() {
        return widgetId;
    }
    
    public void setAnchor(int anchorX, int anchorY) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }
}
