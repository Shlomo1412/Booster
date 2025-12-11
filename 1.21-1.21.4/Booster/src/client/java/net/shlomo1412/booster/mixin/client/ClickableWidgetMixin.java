package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.shlomo1412.booster.client.editor.DraggableWidget;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.editor.widget.ConfigButton;
import net.shlomo1412.booster.client.editor.widget.EditButton;
import net.shlomo1412.booster.client.widget.BoosterButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for ClickableWidget to block interactions with non-Booster buttons in editor mode.
 */
@Mixin(ClickableWidget.class)
public abstract class ClickableWidgetMixin {

    @Shadow
    public abstract int getX();
    
    @Shadow
    public abstract int getY();
    
    @Shadow
    public abstract int getWidth();
    
    @Shadow
    public abstract int getHeight();

    /**
     * Block clicks on non-Booster widgets when editor mode is active.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void booster$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        if (!editor.isEditorModeActive()) {
            return;
        }
        
        ClickableWidget self = (ClickableWidget) (Object) this;
        
        // Allow Booster editor buttons
        if (self instanceof EditButton || self instanceof ConfigButton) {
            return; // Allow these to be clicked normally
        }
        
        // Allow DraggableWidget buttons (Booster module buttons) - they handle their own dragging
        // Don't block them here, but also don't let them do their normal action
        if (self instanceof DraggableWidget || self instanceof BoosterButton) {
            // Block the normal click action - dragging is handled by ScreenMixin
            cir.setReturnValue(false);
            return;
        }
        
        // Block all other widgets (vanilla Minecraft buttons) in editor mode
        cir.setReturnValue(false);
    }

    /**
     * Block mouse release on non-Booster widgets when editor mode is active.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void booster$onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        if (!editor.isEditorModeActive()) {
            return;
        }
        
        ClickableWidget self = (ClickableWidget) (Object) this;
        
        // Allow Booster editor buttons
        if (self instanceof EditButton || self instanceof ConfigButton) {
            return;
        }
        
        // Block all other widgets in editor mode
        cir.setReturnValue(false);
    }
}
