package net.shlomo1412.booster.client.editor;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.shlomo1412.booster.client.editor.widget.EditorSidebar;

/**
 * Handles editor mode mouse events for screens that don't override mouse methods.
 * Uses Fabric's Screen Events API to intercept mouse events.
 */
public class ScreenEditorHandler {
    
    private static boolean initialized = false;
    
    /**
     * Initialize the screen event handlers.
     * Should be called once during mod initialization.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        
        // Register for all screens to handle editor mode
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            // Only handle our target screens
            if (screen instanceof TitleScreen || 
                screen instanceof MultiplayerScreen || 
                screen instanceof GameMenuScreen) {
                
                // Register mouse event handlers for this screen
                registerMouseHandlers(screen);
            }
        });
    }
    
    private static void registerMouseHandlers(Screen screen) {
        // Mouse click handler - use allowMouseClick to be able to cancel
        ScreenMouseEvents.allowMouseClick(screen).register((scr, mouseX, mouseY, button) -> {
            EditorModeManager editor = EditorModeManager.getInstance();
            
            if (!editor.isEditorModeActive()) {
                return true; // Allow click
            }
            
            // First, delegate to sidebar if it exists and mouse is over it
            Element sidebar = editor.getCurrentSidebar();
            if (sidebar != null && sidebar.isMouseOver(mouseX, mouseY)) {
                sidebar.mouseClicked(mouseX, mouseY, button);
                return false; // Block further processing - sidebar handled it
            }
            
            // Check for draggable widget interaction
            DraggableWidget widget = editor.getWidgetAt((int) mouseX, (int) mouseY);
            if (widget != null) {
                DraggableWidget.ResizeEdge edge = widget.getResizeEdge((int) mouseX, (int) mouseY);
                if (edge != null) {
                    editor.startResizing(widget, (int) mouseX, (int) mouseY, edge);
                } else {
                    editor.startDragging(widget, (int) mouseX, (int) mouseY);
                }
                return false; // Block the click
            }
            
            return true; // Allow click
        });
        
        // Mouse release handler
        ScreenMouseEvents.afterMouseRelease(screen).register((scr, mouseX, mouseY, button) -> {
            EditorModeManager editor = EditorModeManager.getInstance();
            
            if (editor.isDragging()) {
                editor.stopDragging();
            }
            if (editor.isResizing()) {
                editor.stopResizing();
            }
        });
        
        // Mouse scroll handler for sidebar
        ScreenMouseEvents.allowMouseScroll(screen).register((scr, mouseX, mouseY, horizontalAmount, verticalAmount) -> {
            EditorModeManager editor = EditorModeManager.getInstance();
            
            if (!editor.isEditorModeActive()) {
                return true; // Allow scroll
            }
            
            // Delegate to sidebar if mouse is over it
            Element sidebar = editor.getCurrentSidebar();
            if (sidebar != null && sidebar.isMouseOver(mouseX, mouseY)) {
                if (sidebar instanceof EditorSidebar editorSidebar) {
                    editorSidebar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
                    return false; // Sidebar consumed the scroll
                }
            }
            
            return true; // Allow scroll
        });
    }
}
