package net.shlomo1412.booster.client.editor;

import net.shlomo1412.booster.client.module.GUIModule;

/**
 * Interface for widgets that can be dragged and resized in editor mode.
 */
public interface DraggableWidget {
    
    // Resize handle size in pixels
    int RESIZE_HANDLE_SIZE = 6;
    int MIN_SIZE = 12;
    int MAX_SIZE = 64;

    /**
     * @return The X position of the widget
     */
    int getX();

    /**
     * @return The Y position of the widget
     */
    int getY();

    /**
     * @return The width of the widget
     */
    int getWidth();

    /**
     * @return The height of the widget
     */
    int getHeight();

    /**
     * Sets the position during editor mode dragging.
     *
     * @param x New X position
     * @param y New Y position
     */
    void setEditorPosition(int x, int y);

    /**
     * Sets the size during editor mode resizing.
     *
     * @param width New width
     * @param height New height
     */
    void setEditorSize(int width, int height);

    /**
     * Saves the current position and size to the module config.
     */
    void savePosition();

    /**
     * @return The module this widget belongs to
     */
    GUIModule getModule();

    /**
     * @return A display name for this widget
     */
    String getDisplayName();

    /**
     * @return Whether this widget supports resizing
     */
    default boolean isResizable() {
        return true;
    }

    /**
     * Checks if the mouse is over this widget.
     *
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @return True if mouse is over this widget
     */
    default boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + getHeight();
    }

    /**
     * Gets the resize edge at the given mouse position.
     * @return ResizeEdge enum value or null if not on an edge
     */
    default ResizeEdge getResizeEdge(int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY) && !isNearEdge(mouseX, mouseY)) {
            return null;
        }

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        boolean onLeft = mouseX >= x - RESIZE_HANDLE_SIZE && mouseX < x + RESIZE_HANDLE_SIZE;
        boolean onRight = mouseX >= x + w - RESIZE_HANDLE_SIZE && mouseX < x + w + RESIZE_HANDLE_SIZE;
        boolean onTop = mouseY >= y - RESIZE_HANDLE_SIZE && mouseY < y + RESIZE_HANDLE_SIZE;
        boolean onBottom = mouseY >= y + h - RESIZE_HANDLE_SIZE && mouseY < y + h + RESIZE_HANDLE_SIZE;

        // Corners first
        if (onTop && onLeft) return ResizeEdge.TOP_LEFT;
        if (onTop && onRight) return ResizeEdge.TOP_RIGHT;
        if (onBottom && onLeft) return ResizeEdge.BOTTOM_LEFT;
        if (onBottom && onRight) return ResizeEdge.BOTTOM_RIGHT;

        // Then edges
        if (onLeft) return ResizeEdge.LEFT;
        if (onRight) return ResizeEdge.RIGHT;
        if (onTop) return ResizeEdge.TOP;
        if (onBottom) return ResizeEdge.BOTTOM;

        return null;
    }

    /**
     * Checks if mouse is near an edge for resize detection.
     */
    default boolean isNearEdge(int mouseX, int mouseY) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int hs = RESIZE_HANDLE_SIZE;

        return mouseX >= x - hs && mouseX < x + w + hs &&
               mouseY >= y - hs && mouseY < y + h + hs;
    }

    /**
     * Enum representing resize edges and corners.
     */
    enum ResizeEdge {
        TOP, BOTTOM, LEFT, RIGHT,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
}
