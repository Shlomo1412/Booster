package net.shlomo1412.booster.client.editor;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.shlomo1412.booster.client.module.GUIModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages the editor mode state for Booster.
 * When editor mode is active, users can drag widgets and modify module settings.
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
     * Updates the position of the dragged widget.
     */
    public void updateDragging(int mouseX, int mouseY) {
        if (isDragging && selectedWidget != null) {
            int newX = mouseX - dragOffsetX;
            int newY = mouseY - dragOffsetY;
            selectedWidget.setEditorPosition(newX, newY);
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
            selectedWidget.savePosition();
        }
        this.isDragging = false;
    }
    
    /**
     * Stops resizing and saves the new size.
     */
    public void stopResizing() {
        if (isResizing && selectedWidget != null) {
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
}
