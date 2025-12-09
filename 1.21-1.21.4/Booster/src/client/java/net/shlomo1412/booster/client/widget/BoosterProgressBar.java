package net.shlomo1412.booster.client.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.shlomo1412.booster.client.editor.DraggableWidget;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.modules.InventoryProgressModule;
import net.shlomo1412.booster.client.module.modules.InventoryProgressModule.FillDirection;

/**
 * A custom widget for the inventory progress bar that implements DraggableWidget.
 * This allows drag-and-resize support in editor mode.
 */
public class BoosterProgressBar implements Drawable, Element, DraggableWidget {
    
    private final InventoryProgressModule module;
    private final HandledScreen<?> screen;
    
    private int x, y, width, height;
    private int anchorX, anchorY;
    private boolean focused = false;
    
    public BoosterProgressBar(InventoryProgressModule module, HandledScreen<?> screen, 
                               int x, int y, int width, int height, int anchorX, int anchorY) {
        this.module = module;
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        
        // Register with editor manager
        EditorModeManager.getInstance().registerDraggableWidget(this);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Calculate fill percentage
        float fillPercent = calculateFillPercentage();
        
        // Get colors from module settings
        int bgColor = module.getBackgroundColorSetting().getValue();
        int fgColor = module.getFillColorSetting().getValue();
        FillDirection direction = module.getFillDirectionSetting().getValue();
        
        // Draw background
        context.fill(x, y, x + width, y + height, bgColor);
        
        // Draw fill based on direction
        int fillX1 = x, fillY1 = y, fillX2 = x + width, fillY2 = y + height;
        
        switch (direction) {
            case UP -> {
                int fillHeight = (int) (height * fillPercent);
                fillY1 = y + height - fillHeight;
                fillY2 = y + height;
            }
            case DOWN -> {
                int fillHeight = (int) (height * fillPercent);
                fillY1 = y;
                fillY2 = y + fillHeight;
            }
            case LEFT -> {
                int fillWidth = (int) (width * fillPercent);
                fillX1 = x + width - fillWidth;
                fillX2 = x + width;
            }
            case RIGHT -> {
                int fillWidth = (int) (width * fillPercent);
                fillX1 = x;
                fillX2 = x + fillWidth;
            }
        }
        
        // Draw fill
        if (fillPercent > 0) {
            context.fill(fillX1, fillY1, fillX2, fillY2, fgColor);
        }
        
        // Draw border
        int borderColor = 0xFF000000;
        context.fill(x, y, x + width, y + 1, borderColor);
        context.fill(x, y + height - 1, x + width, y + height, borderColor);
        context.fill(x, y, x + 1, y + height, borderColor);
        context.fill(x + width - 1, y, x + width, y + height, borderColor);
        
        // Draw editor mode highlight
        EditorModeManager editor = EditorModeManager.getInstance();
        if (editor.isEditorModeActive()) {
            boolean isSelected = editor.getSelectedWidget() == this;
            boolean isHoveredInEditor = isMouseOver(mouseX, mouseY);
            DraggableWidget.ResizeEdge hoverEdge = getResizeEdge(mouseX, mouseY);

            // Draw highlight border
            int editorBorderColor = isSelected ? 0xFFFFAA00 : (isHoveredInEditor ? 0xFF888888 : 0xFF444444);
            context.fill(x - 1, y - 1, x + width + 1, y, editorBorderColor);
            context.fill(x - 1, y + height, x + width + 1, y + height + 1, editorBorderColor);
            context.fill(x - 1, y, x, y + height, editorBorderColor);
            context.fill(x + width, y, x + width + 1, y + height, editorBorderColor);

            // Draw selection border and resize handles when selected
            if (isSelected) {
                // Outer selection border
                context.fill(x - 2, y - 2, x + width + 2, y - 1, 0xFFFFAA00);
                context.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, 0xFFFFAA00);
                context.fill(x - 2, y - 1, x - 1, y + height + 1, 0xFFFFAA00);
                context.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, 0xFFFFAA00);

                // Draw resize handles
                int handleSize = RESIZE_HANDLE_SIZE;
                drawResizeHandle(context, x - handleSize/2, y - handleSize/2, handleSize);
                drawResizeHandle(context, x + width - handleSize/2, y - handleSize/2, handleSize);
                drawResizeHandle(context, x - handleSize/2, y + height - handleSize/2, handleSize);
                drawResizeHandle(context, x + width - handleSize/2, y + height - handleSize/2, handleSize);
            }
        }
    }
    
    private void drawResizeHandle(DrawContext context, int hx, int hy, int size) {
        context.fill(hx, hy, hx + size, hy + size, 0xFFFFAA00);
        context.fill(hx + 1, hy + 1, hx + size - 1, hy + size - 1, 0xFFFFFFFF);
    }
    
    /**
     * Calculates the fill percentage of the container.
     */
    private float calculateFillPercentage() {
        if (screen == null) {
            return 0f;
        }
        
        var handler = screen.getScreenHandler();
        int totalSlots = 0;
        int filledSlots = 0;
        
        for (Slot slot : handler.slots) {
            if (!(slot.inventory instanceof PlayerInventory)) {
                totalSlots++;
                if (slot.hasStack()) {
                    filledSlots++;
                }
            }
        }
        
        if (totalSlots == 0) {
            return 0f;
        }
        
        return (float) filledSlots / totalSlots;
    }
    
    // ==================== DraggableWidget Implementation ====================
    
    @Override
    public int getX() { return x; }
    
    @Override
    public int getY() { return y; }
    
    @Override
    public int getWidth() { return width; }
    
    @Override
    public int getHeight() { return height; }
    
    @Override
    public void setEditorPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public void setEditorSize(int width, int height) {
        // No upper limit - allow any size the user wants
        this.width = Math.max(MIN_SIZE, width);
        this.height = Math.max(MIN_SIZE, height);
    }
    
    @Override
    public void savePosition() {
        int offsetX = x - anchorX;
        int offsetY = y - anchorY;
        module.updateWidgetOffset(InventoryProgressModule.PROGRESS_WIDGET_ID, offsetX, offsetY);
        module.updateWidgetSize(InventoryProgressModule.PROGRESS_WIDGET_ID, width, height);
    }
    
    @Override
    public GUIModule getModule() {
        return module;
    }
    
    @Override
    public String getDisplayName() {
        return "Progress Bar";
    }
    
    @Override
    public boolean isResizable() {
        return true;
    }
    
    // ==================== Element Implementation ====================
    
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }
    
    @Override
    public boolean isFocused() {
        return focused;
    }
    
    public void setAnchor(int anchorX, int anchorY) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }
}
