package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Screen to handle editor mode mouse dragging.
 * Since mouseDragged is inherited from ParentElement and not overridden in Screen,
 * we handle continuous drag updates in the render method instead.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    
    /**
     * Update dragging/resizing state during render when mouse is held down.
     * This is a workaround since we can't inject into mouseDragged.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void booster$onRenderUpdateDrag(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        if (!editor.isEditorModeActive()) {
            return;
        }
        
        // Check if mouse button is held
        MinecraftClient client = MinecraftClient.getInstance();
        Mouse mouse = client.mouse;
        boolean leftMouseDown = mouse.wasLeftButtonClicked();
        
        // If not dragging or resizing, nothing to do
        if (!editor.isDragging() && !editor.isResizing()) {
            return;
        }
        
        // Update drag position
        if (editor.isDragging()) {
            editor.updateDragging(mouseX, mouseY);
        }
        
        // Update resize
        if (editor.isResizing()) {
            editor.updateResizing(mouseX, mouseY);
        }
    }
}
