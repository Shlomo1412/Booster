package net.shlomo1412.booster.client.editor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Renders editor mode overlay elements like grid, alignment guides,
 * center lines, and status information.
 */
public class EditorOverlay {
    
    private static final int GRID_COLOR = 0x20FFFFFF;  // Very subtle white
    private static final int GRID_MAJOR_COLOR = 0x40FFFFFF;  // Slightly visible for major lines
    private static final int ALIGNMENT_GUIDE_COLOR = 0xFFFF5500;  // Orange
    private static final int CENTER_LINE_COLOR = 0x8000AAFF;  // Blue
    private static final int WIDGET_BOUNDS_COLOR = 0xFFFFAA00;  // Gold
    private static final int WIDGET_SELECTED_COLOR = 0xFF00FF00;  // Green
    private static boolean toolsExpanded = false;
    
    /**
     * Renders all editor overlay elements.
     */
    public static void render(DrawContext context, int screenWidth, int screenHeight) {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        if (!editor.isEditorModeActive()) {
            return;
        }
        
        // Update screen dimensions
        editor.setScreenDimensions(screenWidth, screenHeight);
        
        // Render grid (behind everything)
        if (editor.isShowGrid()) {
            renderGrid(context, screenWidth, screenHeight, editor.getGridSize());
        }
        
        // Render center lines
        if (editor.isShowCenterLines()) {
            renderCenterLines(context, screenWidth, screenHeight);
        }
        
        // Render alignment guides (when dragging)
        if (editor.isShowAlignmentGuides() && (editor.isDragging() || editor.isResizing())) {
            renderAlignmentGuides(context, screenWidth, screenHeight, editor);
        }
        
        // Render widget bounds
        renderWidgetBounds(context, editor);
        
        // Render status bar
        renderStatusBar(context, screenWidth, screenHeight, editor);
    }

    public static boolean handleMouseClick(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        EditorModeManager editor = EditorModeManager.getInstance();
        if (!editor.isEditorModeActive()) {
            return false;
        }

        int barHeight = 20;
        int barY = screenHeight - barHeight;
        int buttonWidth = 96;
        int buttonHeight = 14;
        int buttonX = 8;
        int buttonY = barY + 3;

        if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
            toolsExpanded = !toolsExpanded;
            return true;
        }

        if (!toolsExpanded) {
            return false;
        }

        int itemWidth = 130;
        int itemHeight = 14;
        int itemX = buttonX;
        int itemY = buttonY - itemHeight - 2;
        if (mouseX >= itemX && mouseX <= itemX + itemWidth && mouseY >= itemY && mouseY <= itemY + itemHeight) {
            editor.setShowAlignmentGuides(!editor.isShowAlignmentGuides());
            return true;
        }

        itemY -= itemHeight + 2;
        if (mouseX >= itemX && mouseX <= itemX + itemWidth && mouseY >= itemY && mouseY <= itemY + itemHeight) {
            editor.setSnapToGrid(!editor.isSnapToGrid());
            return true;
        }

        itemY -= itemHeight + 2;
        if (mouseX >= itemX && mouseX <= itemX + itemWidth && mouseY >= itemY && mouseY <= itemY + itemHeight) {
            editor.setShowGrid(!editor.isShowGrid());
            return true;
        }

        itemY -= itemHeight + 2;
        if (mouseX >= itemX && mouseX <= itemX + itemWidth && mouseY >= itemY && mouseY <= itemY + itemHeight) {
            editor.setShowCenterLines(!editor.isShowCenterLines());
            return true;
        }

        return false;
    }
    
    /**
     * Renders the grid overlay.
     */
    private static void renderGrid(DrawContext context, int width, int height, int gridSize) {
        EditorModeManager editor = EditorModeManager.getInstance();

        // Vertical lines
        for (int x = 0; x < width; x += gridSize) {
            boolean isMajor = (x % (gridSize * 4)) == 0;
            int color = isMajor ? GRID_MAJOR_COLOR : GRID_COLOR;
            int segStart = -1;
            for (int y = 0; y < height; y++) {
                boolean blocked = isInsideAnyWidget(editor, x, y);
                if (!blocked && segStart == -1) {
                    segStart = y;
                } else if (blocked && segStart != -1) {
                    context.fill(x, segStart, x + 1, y, color);
                    segStart = -1;
                }
            }
            if (segStart != -1) {
                context.fill(x, segStart, x + 1, height, color);
            }
        }
        
        // Horizontal lines
        for (int y = 0; y < height; y += gridSize) {
            boolean isMajor = (y % (gridSize * 4)) == 0;
            int color = isMajor ? GRID_MAJOR_COLOR : GRID_COLOR;
            int segStart = -1;
            for (int x = 0; x < width; x++) {
                boolean blocked = isInsideAnyWidget(editor, x, y);
                if (!blocked && segStart == -1) {
                    segStart = x;
                } else if (blocked && segStart != -1) {
                    context.fill(segStart, y, x, y + 1, color);
                    segStart = -1;
                }
            }
            if (segStart != -1) {
                context.fill(segStart, y, width, y + 1, color);
            }
        }
    }

    private static boolean isInsideAnyWidget(EditorModeManager editor, int x, int y) {
        for (DraggableWidget widget : editor.getDraggableWidgets()) {
            if (x >= widget.getX() && x < widget.getX() + widget.getWidth() &&
                    y >= widget.getY() && y < widget.getY() + widget.getHeight()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Renders center cross lines.
     */
    private static void renderCenterLines(DrawContext context, int width, int height) {
        int centerX = width / 2;
        int centerY = height / 2;
        
        // Vertical center line (dashed effect)
        for (int y = 0; y < height; y += 6) {
            context.fill(centerX, y, centerX + 1, Math.min(y + 4, height), CENTER_LINE_COLOR);
        }
        
        // Horizontal center line (dashed effect)
        for (int x = 0; x < width; x += 6) {
            context.fill(x, centerY, Math.min(x + 4, width), centerY + 1, CENTER_LINE_COLOR);
        }
    }
    
    /**
     * Renders alignment guide lines when snapping.
     */
    private static void renderAlignmentGuides(DrawContext context, int width, int height, 
                                               EditorModeManager editor) {
        Integer guideX = editor.getAlignmentGuideX();
        Integer guideY = editor.getAlignmentGuideY();
        
        // Vertical alignment guide
        if (guideX != null) {
            context.fill(guideX, 0, guideX + 1, height, ALIGNMENT_GUIDE_COLOR);
            
            // Draw small triangles at top and bottom
            drawTriangle(context, guideX, 5, true, ALIGNMENT_GUIDE_COLOR);
            drawTriangle(context, guideX, height - 5, false, ALIGNMENT_GUIDE_COLOR);
        }
        
        // Horizontal alignment guide
        if (guideY != null) {
            context.fill(0, guideY, width, guideY + 1, ALIGNMENT_GUIDE_COLOR);
            
            // Draw small triangles at left and right
            drawTriangleHorizontal(context, 5, guideY, true, ALIGNMENT_GUIDE_COLOR);
            drawTriangleHorizontal(context, width - 5, guideY, false, ALIGNMENT_GUIDE_COLOR);
        }
    }
    
    /**
     * Draws a small triangle pointing up or down.
     */
    private static void drawTriangle(DrawContext context, int x, int y, boolean pointDown, int color) {
        if (pointDown) {
            context.fill(x - 3, y - 3, x + 4, y - 2, color);
            context.fill(x - 2, y - 2, x + 3, y - 1, color);
            context.fill(x - 1, y - 1, x + 2, y, color);
            context.fill(x, y, x + 1, y + 1, color);
        } else {
            context.fill(x, y, x + 1, y + 1, color);
            context.fill(x - 1, y + 1, x + 2, y + 2, color);
            context.fill(x - 2, y + 2, x + 3, y + 3, color);
            context.fill(x - 3, y + 3, x + 4, y + 4, color);
        }
    }
    
    /**
     * Draws a small triangle pointing left or right.
     */
    private static void drawTriangleHorizontal(DrawContext context, int x, int y, boolean pointRight, int color) {
        if (pointRight) {
            context.fill(x - 3, y - 3, x - 2, y + 4, color);
            context.fill(x - 2, y - 2, x - 1, y + 3, color);
            context.fill(x - 1, y - 1, x, y + 2, color);
            context.fill(x, y, x + 1, y + 1, color);
        } else {
            context.fill(x, y, x + 1, y + 1, color);
            context.fill(x + 1, y - 1, x + 2, y + 2, color);
            context.fill(x + 2, y - 2, x + 3, y + 3, color);
            context.fill(x + 3, y - 3, x + 4, y + 4, color);
        }
    }
    
    /**
     * Renders bounds for all draggable widgets.
     */
    private static void renderWidgetBounds(DrawContext context, EditorModeManager editor) {
        DraggableWidget selected = editor.getSelectedWidget();
        
        for (DraggableWidget widget : editor.getDraggableWidgets()) {
            int x = widget.getX();
            int y = widget.getY();
            int w = widget.getWidth();
            int h = widget.getHeight();
            
            boolean isSelected = widget == selected;
            int color = isSelected ? WIDGET_SELECTED_COLOR : WIDGET_BOUNDS_COLOR;
            int alpha = isSelected ? 0xFF : 0x80;
            color = (color & 0x00FFFFFF) | (alpha << 24);
            
            // Draw border
            context.fill(x, y, x + w, y + 1, color);  // Top
            context.fill(x, y + h - 1, x + w, y + h, color);  // Bottom
            context.fill(x, y, x + 1, y + h, color);  // Left
            context.fill(x + w - 1, y, x + w, y + h, color);  // Right
            
            // Draw resize handles for selected widget
            if (isSelected && widget.isResizable()) {
                int hs = DraggableWidget.RESIZE_HANDLE_SIZE;
                int handleColor = 0xFFFFFFFF;
                
                // Corner handles
                context.fill(x - hs/2, y - hs/2, x + hs/2, y + hs/2, handleColor);  // Top-left
                context.fill(x + w - hs/2, y - hs/2, x + w + hs/2, y + hs/2, handleColor);  // Top-right
                context.fill(x - hs/2, y + h - hs/2, x + hs/2, y + h + hs/2, handleColor);  // Bottom-left
                context.fill(x + w - hs/2, y + h - hs/2, x + w + hs/2, y + h + hs/2, handleColor);  // Bottom-right
            }
        }
    }
    
    /**
     * Renders the editor status bar at the bottom of the screen.
     */
    private static void renderStatusBar(DrawContext context, int width, int height, 
                                         EditorModeManager editor) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        
        int barHeight = 20;
        int barY = height - barHeight;
        
        // Background
        context.fill(0, barY, width, height, 0xDD000000);
        context.fill(0, barY, width, barY + 1, 0xFFFFAA00);
        
        // Left side: Status info
        int textY = barY + 6;
        int textX = 8;

        // Editor tools drop-up menu
        int toolsButtonX = textX;
        int toolsButtonY = barY + 3;
        int toolsButtonW = 96;
        int toolsButtonH = 14;
        context.fill(toolsButtonX, toolsButtonY, toolsButtonX + toolsButtonW, toolsButtonY + toolsButtonH, 0xCC1E1E1E);
        context.fill(toolsButtonX, toolsButtonY, toolsButtonX + toolsButtonW, toolsButtonY + 1, 0xFF444444);
        context.fill(toolsButtonX, toolsButtonY + toolsButtonH - 1, toolsButtonX + toolsButtonW, toolsButtonY + toolsButtonH, 0xFF444444);
        context.drawTextWithShadow(textRenderer,
            toolsExpanded ? "Editor Tools v" : "Editor Tools ^",
            toolsButtonX + 4, toolsButtonY + 3, 0xFFE5E5E5);

        if (toolsExpanded) {
            int itemX = toolsButtonX;
            int itemY = toolsButtonY - 16;
            itemY = renderToolItem(context, textRenderer, itemX, itemY, 130, "[A] Guides", editor.isShowAlignmentGuides());
            itemY = renderToolItem(context, textRenderer, itemX, itemY, 130, "[S] Snap", editor.isSnapToGrid());
            itemY = renderToolItem(context, textRenderer, itemX, itemY, 130, "[G] Grid", editor.isShowGrid());
            renderToolItem(context, textRenderer, itemX, itemY, 130, "[C] Center", editor.isShowCenterLines());
        }

        textX += toolsButtonW + 10;
        
        // Editor mode indicator
        context.drawTextWithShadow(textRenderer, "§6§lEDITOR", textX, textY, 0xFFFFAA00);
        textX += textRenderer.getWidth("EDITOR") + 10;
        
        String centerText = editor.isShowCenterLines() ? "§aCenter: ON" : "§7Center: OFF";
        context.drawTextWithShadow(textRenderer, centerText, textX, textY, 0xFFFFFF);
        textX += textRenderer.getWidth("Center: OFF") + 12;
        
        // Separator
        context.fill(textX, barY + 4, textX + 1, height - 4, 0x80FFFFFF);
        textX += 10;
        
        // Undo/Redo count
        String undoText = "Undo: " + editor.getUndoCount();
        String redoText = "Redo: " + editor.getRedoCount();
        context.drawTextWithShadow(textRenderer, 
            editor.canUndo() ? "§a" + undoText : "§7" + undoText, textX, textY, 0xFFFFFF);
        textX += textRenderer.getWidth(undoText) + 8;
        context.drawTextWithShadow(textRenderer, 
            editor.canRedo() ? "§a" + redoText : "§7" + redoText, textX, textY, 0xFFFFFF);
        
        // Right side: Keyboard shortcuts hint
        String shortcuts = "§7Ctrl+Z/Y: Undo/Redo §8| §7Arrows: Nudge §8| §7Del: Reset";
        int shortcutsWidth = textRenderer.getWidth(shortcuts.replaceAll("§.", ""));
        context.drawTextWithShadow(textRenderer, shortcuts, width - shortcutsWidth - 8, textY, 0xFFFFFF);
        
        // Selected widget info (in center)
        DraggableWidget selected = editor.getSelectedWidget();
        if (selected != null) {
            String info = String.format("§e%s §7[%d, %d] %dx%d", 
                selected.getDisplayName(), 
                selected.getX(), selected.getY(),
                selected.getWidth(), selected.getHeight());
            int infoWidth = textRenderer.getWidth(info.replaceAll("§.", ""));
            int infoX = (width - infoWidth) / 2;
            context.drawTextWithShadow(textRenderer, info, infoX, textY, 0xFFFFFF);
        }
    }

    private static int renderToolItem(DrawContext context, TextRenderer textRenderer,
                                      int x, int y, int width, String label, boolean enabled) {
        int itemY = y;
        context.fill(x, itemY, x + width, itemY + 14, 0xDD1A1A1A);
        int accent = enabled ? 0xFF44BB44 : 0xFF666666;
        context.fill(x, itemY, x + 2, itemY + 14, accent);
        context.drawTextWithShadow(textRenderer,
            (enabled ? "§a" : "§7") + label,
            x + 5, itemY + 3, 0xFFFFFF);
        return y - 16;
    }
}
