package net.shlomo1412.booster.client.editor;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

/**
 * Manages the editor mode state for Booster.
 * When editor mode is active, users can drag widgets and modify module settings.
 * Features: Undo/Redo, grid snapping, alignment guides, nudge with arrow keys.
 */
public class EditorModeManager {
    private static EditorModeManager instance;

    private boolean editorModeActive = false;
    private Screen currentScreen = null;
    private DraggableWidget selectedWidget = null;
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    // Resize state
    private boolean isResizing = false;
    private DraggableWidget.ResizeEdge resizeEdge = null;
    private int resizeStartX = 0;
    private int resizeStartY = 0;
    private int resizeStartWidth = 0;
    private int resizeStartHeight = 0;
    private int resizeStartWidgetX = 0;
    private int resizeStartWidgetY = 0;

    // List of all draggable widgets on the current screen
    private final List<DraggableWidget> draggableWidgets = new ArrayList<>();

    // Listeners for editor mode changes
    private final List<Consumer<Boolean>> editorModeListeners = new ArrayList<>();

    // Current sidebar element for mouse event delegation
    private Element currentSidebar = null;
    
    // ===== UNDO/REDO SYSTEM =====
    private final Stack<EditorAction> undoStack = new Stack<>();
    private final Stack<EditorAction> redoStack = new Stack<>();
    private static final int MAX_UNDO_HISTORY = 50;
    
    // ===== EDITOR SETTINGS =====
    private boolean showGrid = true;
    private boolean snapToGrid = true;
    private int gridSize = 8;  // Pixels per grid cell
    private boolean showAlignmentGuides = true;
    private boolean showCenterLines = true;
    private int alignmentSnapDistance = 5;  // Pixels to snap to alignment
    
    // ===== ALIGNMENT GUIDE STATE =====
    private Integer alignmentGuideX = null;  // Current vertical alignment line
    private Integer alignmentGuideY = null;  // Current horizontal alignment line
    private boolean alignedToCenter = false;
    
    // Screen dimensions for centering
    private int screenWidth = 0;
    private int screenHeight = 0;

    private EditorModeManager() {
    }

    public static EditorModeManager getInstance() {
        if (instance == null) {
            instance = new EditorModeManager();
        }
        return instance;
    }

    /**
     * Toggles editor mode on/off.
     */
    public void toggleEditorMode() {
        setEditorModeActive(!editorModeActive);
    }

    /**
     * Sets whether editor mode is active.
     */
    public void setEditorModeActive(boolean active) {
        if (this.editorModeActive != active) {
            this.editorModeActive = active;
            
            if (!active) {
                // Clear selection when exiting editor mode
                selectedWidget = null;
                isDragging = false;
            }

            // Notify listeners
            for (Consumer<Boolean> listener : editorModeListeners) {
                listener.accept(active);
            }
        }
    }

    /**
     * @return Whether editor mode is currently active
     */
    public boolean isEditorModeActive() {
        return editorModeActive;
    }

    /**
     * Sets the current screen being edited.
     */
    public void setCurrentScreen(Screen screen) {
        if (this.currentScreen != screen) {
            this.currentScreen = screen;
            this.draggableWidgets.clear();
            this.selectedWidget = null;
            this.isDragging = false;
            
            // Auto-disable editor mode when screen changes
            if (editorModeActive) {
                setEditorModeActive(false);
            }
        }
    }

    /**
     * @return The current screen being edited
     */
    public Screen getCurrentScreen() {
        return currentScreen;
    }

    /**
     * Registers a draggable widget for the current screen.
     */
    public void registerDraggableWidget(DraggableWidget widget) {
        if (!draggableWidgets.contains(widget)) {
            draggableWidgets.add(widget);
        }
    }

    /**
     * Unregisters a draggable widget.
     */
    public void unregisterDraggableWidget(DraggableWidget widget) {
        draggableWidgets.remove(widget);
        if (selectedWidget == widget) {
            selectedWidget = null;
        }
    }

    /**
     * Clears all draggable widgets.
     */
    public void clearDraggableWidgets() {
        draggableWidgets.clear();
        selectedWidget = null;
    }

    /**
     * @return All draggable widgets on the current screen
     */
    public List<DraggableWidget> getDraggableWidgets() {
        return draggableWidgets;
    }

    /**
     * @return The currently selected widget, or null if none
     */
    public DraggableWidget getSelectedWidget() {
        return selectedWidget;
    }

    /**
     * Sets the selected widget.
     */
    public void setSelectedWidget(DraggableWidget widget) {
        this.selectedWidget = widget;
    }

    /**
     * Starts dragging a widget.
     */
    public void startDragging(DraggableWidget widget, int mouseX, int mouseY) {
        this.selectedWidget = widget;
        this.isDragging = true;
        this.isResizing = false;
        this.dragOffsetX = mouseX - widget.getX();
        this.dragOffsetY = mouseY - widget.getY();
        
        // Store starting position for undo
        this.resizeStartWidgetX = widget.getX();
        this.resizeStartWidgetY = widget.getY();
        this.resizeStartWidth = widget.getWidth();
        this.resizeStartHeight = widget.getHeight();
    }

    /**
     * Starts resizing a widget.
     */
    public void startResizing(DraggableWidget widget, int mouseX, int mouseY, DraggableWidget.ResizeEdge edge) {
        this.selectedWidget = widget;
        this.isResizing = true;
        this.isDragging = false;
        this.resizeEdge = edge;
        this.resizeStartX = mouseX;
        this.resizeStartY = mouseY;
        this.resizeStartWidth = widget.getWidth();
        this.resizeStartHeight = widget.getHeight();
        this.resizeStartWidgetX = widget.getX();
        this.resizeStartWidgetY = widget.getY();
    }

    /**
     * Updates the position of the dragged widget with snapping.
     */
    public void updateDragging(int mouseX, int mouseY) {
        if (isDragging && selectedWidget != null) {
            int rawX = mouseX - dragOffsetX;
            int rawY = mouseY - dragOffsetY;
            
            // Apply alignment guides and grid snapping
            int[] snapped = snapPositionWithGuides(rawX, rawY, selectedWidget.getWidth(), selectedWidget.getHeight());
            selectedWidget.setEditorPosition(snapped[0], snapped[1]);
        }
    }

    /**
     * Updates the size of the resized widget.
     */
    public void updateResizing(int mouseX, int mouseY) {
        if (!isResizing || selectedWidget == null || resizeEdge == null) {
            return;
        }

        int deltaX = mouseX - resizeStartX;
        int deltaY = mouseY - resizeStartY;

        int newX = resizeStartWidgetX;
        int newY = resizeStartWidgetY;
        int newWidth = resizeStartWidth;
        int newHeight = resizeStartHeight;

        switch (resizeEdge) {
            case LEFT:
                newX = resizeStartWidgetX + deltaX;
                newWidth = resizeStartWidth - deltaX;
                break;
            case RIGHT:
                newWidth = resizeStartWidth + deltaX;
                break;
            case TOP:
                newY = resizeStartWidgetY + deltaY;
                newHeight = resizeStartHeight - deltaY;
                break;
            case BOTTOM:
                newHeight = resizeStartHeight + deltaY;
                break;
            case TOP_LEFT:
                newX = resizeStartWidgetX + deltaX;
                newY = resizeStartWidgetY + deltaY;
                newWidth = resizeStartWidth - deltaX;
                newHeight = resizeStartHeight - deltaY;
                break;
            case TOP_RIGHT:
                newY = resizeStartWidgetY + deltaY;
                newWidth = resizeStartWidth + deltaX;
                newHeight = resizeStartHeight - deltaY;
                break;
            case BOTTOM_LEFT:
                newX = resizeStartWidgetX + deltaX;
                newWidth = resizeStartWidth - deltaX;
                newHeight = resizeStartHeight + deltaY;
                break;
            case BOTTOM_RIGHT:
                newWidth = resizeStartWidth + deltaX;
                newHeight = resizeStartHeight + deltaY;
                break;
        }

        // Clamp size
        // Only enforce minimum size - no maximum limit to allow user customization
        newWidth = Math.max(DraggableWidget.MIN_SIZE, newWidth);
        newHeight = Math.max(DraggableWidget.MIN_SIZE, newHeight);

        // Adjust position if resizing from left or top
        if (resizeEdge == DraggableWidget.ResizeEdge.LEFT || 
            resizeEdge == DraggableWidget.ResizeEdge.TOP_LEFT || 
            resizeEdge == DraggableWidget.ResizeEdge.BOTTOM_LEFT) {
            newX = resizeStartWidgetX + resizeStartWidth - newWidth;
        }
        if (resizeEdge == DraggableWidget.ResizeEdge.TOP || 
            resizeEdge == DraggableWidget.ResizeEdge.TOP_LEFT || 
            resizeEdge == DraggableWidget.ResizeEdge.TOP_RIGHT) {
            newY = resizeStartWidgetY + resizeStartHeight - newHeight;
        }

        selectedWidget.setEditorPosition(newX, newY);
        selectedWidget.setEditorSize(newWidth, newHeight);
    }

    /**
     * Stops dragging and saves the new position.
     */
    public void stopDragging() {
        if (isDragging && selectedWidget != null) {
            // Record action for undo
            recordAction(selectedWidget, 
                resizeStartWidgetX, resizeStartWidgetY, resizeStartWidth, resizeStartHeight,
                selectedWidget.getX(), selectedWidget.getY(), selectedWidget.getWidth(), selectedWidget.getHeight());
            
            selectedWidget.savePosition();
            clearAlignmentGuides();
        }
        this.isDragging = false;
    }
    
    /**
     * Stops resizing and saves the new size.
     */
    public void stopResizing() {
        if (isResizing && selectedWidget != null) {
            // Record action for undo
            recordAction(selectedWidget,
                resizeStartWidgetX, resizeStartWidgetY, resizeStartWidth, resizeStartHeight,
                selectedWidget.getX(), selectedWidget.getY(), selectedWidget.getWidth(), selectedWidget.getHeight());
            
            selectedWidget.savePosition();
        }
        this.isResizing = false;
        this.resizeEdge = null;
    }

    /**
     * @return Whether currently dragging a widget
     */
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * @return Whether currently resizing a widget
     */
    public boolean isResizing() {
        return isResizing;
    }

    /**
     * @return The current resize edge, or null if not resizing
     */
    public DraggableWidget.ResizeEdge getResizeEdge() {
        return resizeEdge;
    }

    /**
     * Finds the widget at the given position.
     */
    public DraggableWidget getWidgetAt(int mouseX, int mouseY) {
        // Iterate in reverse to get top-most widget first
        for (int i = draggableWidgets.size() - 1; i >= 0; i--) {
            DraggableWidget widget = draggableWidgets.get(i);
            if (widget.isMouseOver(mouseX, mouseY)) {
                return widget;
            }
        }
        return null;
    }

    /**
     * Adds a listener for editor mode changes.
     */
    public void addEditorModeListener(Consumer<Boolean> listener) {
        editorModeListeners.add(listener);
    }

    /**
     * Removes an editor mode listener.
     */
    public void removeEditorModeListener(Consumer<Boolean> listener) {
        editorModeListeners.remove(listener);
    }

    /**
     * Resets the editor state (call when closing screens).
     */
    public void reset() {
        editorModeActive = false;
        currentScreen = null;
        selectedWidget = null;
        isDragging = false;
        draggableWidgets.clear();
        currentSidebar = null;
        undoStack.clear();
        redoStack.clear();
        clearAlignmentGuides();
    }

    /**
     * Sets the current sidebar element for mouse event delegation.
     */
    public void setCurrentSidebar(Element sidebar) {
        this.currentSidebar = sidebar;
    }

    /**
     * @return The current sidebar element, or null if none
     */
    public Element getCurrentSidebar() {
        return currentSidebar;
    }

    /**
     * Clears the current sidebar.
     */
    public void clearCurrentSidebar() {
        this.currentSidebar = null;
    }
    
    // ===== UNDO/REDO METHODS =====
    
    /**
     * Records an action for undo/redo.
     */
    public void recordAction(DraggableWidget widget, int oldX, int oldY, int oldW, int oldH,
                              int newX, int newY, int newW, int newH) {
        // Don't record if nothing changed
        if (oldX == newX && oldY == newY && oldW == newW && oldH == newH) {
            return;
        }
        
        EditorAction action = new EditorAction(widget, oldX, oldY, oldW, oldH, newX, newY, newW, newH);
        undoStack.push(action);
        
        // Clear redo stack when new action is performed
        redoStack.clear();
        
        // Limit history size
        while (undoStack.size() > MAX_UNDO_HISTORY) {
            undoStack.remove(0);
        }
    }
    
    /**
     * Undoes the last action.
     * @return true if an action was undone
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        
        EditorAction action = undoStack.pop();
        action.undo();
        redoStack.push(action);
        return true;
    }
    
    /**
     * Redoes the last undone action.
     * @return true if an action was redone
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        
        EditorAction action = redoStack.pop();
        action.redo();
        undoStack.push(action);
        return true;
    }
    
    /**
     * @return true if there are actions to undo
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * @return true if there are actions to redo
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * @return The number of actions that can be undone
     */
    public int getUndoCount() {
        return undoStack.size();
    }
    
    /**
     * @return The number of actions that can be redone
     */
    public int getRedoCount() {
        return redoStack.size();
    }
    
    // ===== GRID AND SNAPPING =====
    
    /**
     * Sets the screen dimensions for centering calculations.
     */
    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    public int getScreenWidth() {
        return screenWidth;
    }
    
    public int getScreenHeight() {
        return screenHeight;
    }
    
    public boolean isShowGrid() {
        return showGrid;
    }
    
    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }
    
    public void toggleShowGrid() {
        this.showGrid = !this.showGrid;
    }
    
    public boolean isSnapToGrid() {
        return snapToGrid;
    }
    
    public void setSnapToGrid(boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }
    
    public void toggleSnapToGrid() {
        this.snapToGrid = !this.snapToGrid;
    }
    
    public int getGridSize() {
        return gridSize;
    }
    
    public void setGridSize(int gridSize) {
        this.gridSize = Math.max(4, Math.min(32, gridSize));
    }
    
    public boolean isShowAlignmentGuides() {
        return showAlignmentGuides;
    }
    
    public void setShowAlignmentGuides(boolean show) {
        this.showAlignmentGuides = show;
    }
    
    public void toggleShowAlignmentGuides() {
        this.showAlignmentGuides = !this.showAlignmentGuides;
    }
    
    public boolean isShowCenterLines() {
        return showCenterLines;
    }
    
    public void setShowCenterLines(boolean show) {
        this.showCenterLines = show;
    }
    
    public void toggleShowCenterLines() {
        this.showCenterLines = !this.showCenterLines;
    }
    
    /**
     * Snaps a value to the grid if snapping is enabled.
     */
    public int snapToGrid(int value) {
        if (!snapToGrid) {
            return value;
        }
        return Math.round((float) value / gridSize) * gridSize;
    }
    
    /**
     * Snaps position with alignment guide detection.
     * Returns snapped position and updates alignment guide state.
     */
    public int[] snapPositionWithGuides(int x, int y, int width, int height) {
        int newX = x;
        int newY = y;
        
        clearAlignmentGuides();
        
        // Check center alignment
        int widgetCenterX = x + width / 2;
        int widgetCenterY = y + height / 2;
        int screenCenterX = screenWidth / 2;
        int screenCenterY = screenHeight / 2;
        
        if (showAlignmentGuides) {
            // Snap to screen center X
            if (Math.abs(widgetCenterX - screenCenterX) <= alignmentSnapDistance) {
                newX = screenCenterX - width / 2;
                alignmentGuideX = screenCenterX;
                alignedToCenter = true;
            }
            
            // Snap to screen center Y
            if (Math.abs(widgetCenterY - screenCenterY) <= alignmentSnapDistance) {
                newY = screenCenterY - height / 2;
                alignmentGuideY = screenCenterY;
                alignedToCenter = true;
            }
            
            // Check alignment with other widgets
            for (DraggableWidget other : draggableWidgets) {
                if (other == selectedWidget) continue;
                
                int otherCenterX = other.getX() + other.getWidth() / 2;
                int otherCenterY = other.getY() + other.getHeight() / 2;
                
                // Align centers
                if (Math.abs(widgetCenterX - otherCenterX) <= alignmentSnapDistance) {
                    newX = otherCenterX - width / 2;
                    alignmentGuideX = otherCenterX;
                }
                if (Math.abs(widgetCenterY - otherCenterY) <= alignmentSnapDistance) {
                    newY = otherCenterY - height / 2;
                    alignmentGuideY = otherCenterY;
                }
                
                // Align edges
                // Left edge to left edge
                if (Math.abs(x - other.getX()) <= alignmentSnapDistance) {
                    newX = other.getX();
                    alignmentGuideX = other.getX();
                }
                // Right edge to right edge
                if (Math.abs((x + width) - (other.getX() + other.getWidth())) <= alignmentSnapDistance) {
                    newX = other.getX() + other.getWidth() - width;
                    alignmentGuideX = other.getX() + other.getWidth();
                }
                // Top edge to top edge
                if (Math.abs(y - other.getY()) <= alignmentSnapDistance) {
                    newY = other.getY();
                    alignmentGuideY = other.getY();
                }
                // Bottom edge to bottom edge
                if (Math.abs((y + height) - (other.getY() + other.getHeight())) <= alignmentSnapDistance) {
                    newY = other.getY() + other.getHeight() - height;
                    alignmentGuideY = other.getY() + other.getHeight();
                }
            }
        }
        
        // Apply grid snapping if no alignment was found
        if (snapToGrid) {
            if (alignmentGuideX == null) {
                newX = snapToGrid(newX);
            }
            if (alignmentGuideY == null) {
                newY = snapToGrid(newY);
            }
        }
        
        return new int[] { newX, newY };
    }
    
    public Integer getAlignmentGuideX() {
        return alignmentGuideX;
    }
    
    public Integer getAlignmentGuideY() {
        return alignmentGuideY;
    }
    
    public boolean isAlignedToCenter() {
        return alignedToCenter;
    }
    
    public void clearAlignmentGuides() {
        alignmentGuideX = null;
        alignmentGuideY = null;
        alignedToCenter = false;
    }
    
    // ===== NUDGE (ARROW KEYS) =====
    
    /**
     * Nudges the selected widget by the given amount.
     * @param dx X offset
     * @param dy Y offset
     * @return true if a widget was nudged
     */
    public boolean nudgeSelectedWidget(int dx, int dy) {
        if (selectedWidget == null) {
            return false;
        }
        
        int oldX = selectedWidget.getX();
        int oldY = selectedWidget.getY();
        int newX = oldX + dx;
        int newY = oldY + dy;
        
        // Apply grid snapping for larger nudges
        if (snapToGrid && (Math.abs(dx) >= gridSize || Math.abs(dy) >= gridSize)) {
            newX = snapToGrid(newX);
            newY = snapToGrid(newY);
        }
        
        selectedWidget.setEditorPosition(newX, newY);
        selectedWidget.savePosition();
        
        // Record for undo
        recordAction(selectedWidget, oldX, oldY, selectedWidget.getWidth(), selectedWidget.getHeight(),
                     newX, newY, selectedWidget.getWidth(), selectedWidget.getHeight());
        
        return true;
    }
    
    /**
     * Resets the selected widget to its default position.
     * @return true if a widget was reset
     */
    public boolean resetSelectedWidget() {
        if (selectedWidget == null) {
            return false;
        }
        
        int oldX = selectedWidget.getX();
        int oldY = selectedWidget.getY();
        int oldW = selectedWidget.getWidth();
        int oldH = selectedWidget.getHeight();
        
        // Reset to default (this will be module-specific)
        GUIModule module = selectedWidget.getModule();
        if (module != null) {
            // Reset position to defaults
            module.resetPosition();
            ModuleManager.getInstance().saveConfig();
        }
        
        int newX = selectedWidget.getX();
        int newY = selectedWidget.getY();
        int newW = selectedWidget.getWidth();
        int newH = selectedWidget.getHeight();
        
        // Record for undo
        recordAction(selectedWidget, oldX, oldY, oldW, oldH, newX, newY, newW, newH);
        
        return true;
    }
    
    // ===== EDITOR ACTION CLASS FOR UNDO/REDO =====
    
    /**
     * Represents an editor action that can be undone/redone.
     */
    public static class EditorAction {
        private final DraggableWidget widget;
        private final int oldX, oldY, oldWidth, oldHeight;
        private final int newX, newY, newWidth, newHeight;
        
        public EditorAction(DraggableWidget widget, 
                           int oldX, int oldY, int oldWidth, int oldHeight,
                           int newX, int newY, int newWidth, int newHeight) {
            this.widget = widget;
            this.oldX = oldX;
            this.oldY = oldY;
            this.oldWidth = oldWidth;
            this.oldHeight = oldHeight;
            this.newX = newX;
            this.newY = newY;
            this.newWidth = newWidth;
            this.newHeight = newHeight;
        }
        
        public void undo() {
            widget.setEditorPosition(oldX, oldY);
            widget.setEditorSize(oldWidth, oldHeight);
            widget.savePosition();
        }
        
        public void redo() {
            widget.setEditorPosition(newX, newY);
            widget.setEditorSize(newWidth, newHeight);
            widget.savePosition();
        }
        
        public DraggableWidget getWidget() {
            return widget;
        }
        
        public String getDescription() {
            if (oldX != newX || oldY != newY) {
                if (oldWidth != newWidth || oldHeight != newHeight) {
                    return "Move & Resize " + widget.getDisplayName();
                }
                return "Move " + widget.getDisplayName();
            }
            return "Resize " + widget.getDisplayName();
        }
    }
}
