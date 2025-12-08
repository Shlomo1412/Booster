package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.shlomo1412.booster.client.editor.DraggableWidget;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.editor.ScreenInfo;
import net.shlomo1412.booster.client.editor.widget.ConfigButton;
import net.shlomo1412.booster.client.editor.widget.EditButton;
import net.shlomo1412.booster.client.editor.widget.EditorSidebar;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.StealStoreModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to inject Booster widgets into container screens.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    @Shadow
    @Final
    protected T handler;

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Shadow
    protected int backgroundWidth;

    @Shadow
    protected int backgroundHeight;

    // Editor mode components
    @Unique
    private EditButton booster$editButton;
    
    @Unique
    private ConfigButton booster$configButton;
    
    @Unique
    private EditorSidebar booster$editorSidebar;
    
    @Unique
    private boolean booster$hasBoosterContent = false;

    // Required for extending Screen
    protected HandledScreenMixin() {
        super(null);
    }

    /**
     * Injects after the screen is initialized to add our buttons.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void booster$onInit(CallbackInfo ci) {
        if (!ModuleManager.getInstance().isInitialized()) {
            return;
        }

        // Reset editor state for new screen
        EditorModeManager editor = EditorModeManager.getInstance();
        editor.setCurrentScreen(this);
        editor.clearDraggableWidgets();
        booster$hasBoosterContent = false;

        // Only add Booster content to container screens
        if (!(handler instanceof GenericContainerScreenHandler)) {
            return;
        }

        // Add Steal/Store buttons
        StealStoreModule stealStoreModule = ModuleManager.getInstance().getModule(StealStoreModule.class);
        if (stealStoreModule != null) {
            booster$hasBoosterContent = true;
            
            if (stealStoreModule.isEnabled()) {
                HandledScreen<?> self = (HandledScreen<?>) (Object) this;
                // Pass right edge of container as anchor for buttons
                stealStoreModule.createButtons(
                    self,
                    x + backgroundWidth,  // Right edge of container
                    y,
                    button -> this.addDrawableChild(button)
                );
            }
        }

        // Add Edit button if we have Booster content
        if (booster$hasBoosterContent) {
            // Position config button at top-right of container
            int configX = x + backgroundWidth + 4;
            int configY = y - 24;
            
            booster$configButton = ConfigButton.create(configX, configY, this);
            this.addDrawableChild(booster$configButton);
            
            // Position edit button next to config button (both are 20px wide)
            int editX = configX + 22;  // 20px button + 2px gap
            int editY = configY;
            
            booster$editButton = EditButton.create(editX, editY, this::booster$onEditorToggle);
            this.addDrawableChild(booster$editButton);
        }
    }

    /**
     * Called when editor mode is toggled.
     */
    @Unique
    private void booster$onEditorToggle() {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        if (editor.isEditorModeActive()) {
            // Create sidebar
            List<GUIModule> activeModules = new ArrayList<>();
            
            StealStoreModule stealStore = ModuleManager.getInstance().getModule(StealStoreModule.class);
            if (stealStore != null) {
                activeModules.add(stealStore);
            }
            
            ScreenInfo screenInfo = new ScreenInfo(this, x, y, backgroundWidth, backgroundHeight);
            booster$editorSidebar = new EditorSidebar(
                MinecraftClient.getInstance(), 
                screenInfo, 
                activeModules,
                element -> {}
            );
        } else {
            if (booster$editorSidebar != null) {
                booster$editorSidebar.close();
            }
        }
    }

    /**
     * Render editor overlay LAST so it appears above all other elements.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void booster$onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        if (booster$editorSidebar != null) {
            // Check if sidebar should be removed after close animation
            if (booster$editorSidebar.isClosed()) {
                booster$editorSidebar = null;
                return;
            }
            
            // Dim the area to the RIGHT of the sidebar (since sidebar is on left)
            int sidebarRightEdge = booster$editorSidebar.getRightEdge();
            if (sidebarRightEdge > 0) {
                context.fill(sidebarRightEdge, 0, this.width, this.height, 0x60000000);
            }
            
            // Draw "EDITOR MODE" indicator at top center
            if (editor.isEditorModeActive()) {
                context.drawCenteredTextWithShadow(this.textRenderer, 
                        "§6§lEDITOR MODE", this.width / 2, 8, 0xFFFFAA00);
            }
            
            // Render sidebar LAST so it appears above everything
            booster$editorSidebar.render(context, mouseX, mouseY, delta);
        }
    }

    /**
     * Handle mouse scroll for sidebar.
     */
    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void booster$onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (booster$editorSidebar != null && booster$editorSidebar.isMouseOver(mouseX, mouseY)) {
            if (booster$editorSidebar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Handle mouse clicks for editor mode.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void booster$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        if (!editor.isEditorModeActive()) {
            return;
        }

        // Check sidebar first
        if (booster$editorSidebar != null && booster$editorSidebar.isMouseOver(mouseX, mouseY)) {
            if (booster$editorSidebar.mouseClicked(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        }

        // Check edit button (always allow clicking)
        if (booster$editButton != null && booster$editButton.isMouseOver(mouseX, mouseY)) {
            return; // Let the normal click handling work
        }

        // Check draggable widgets for resize or drag
        DraggableWidget widget = editor.getWidgetAt((int) mouseX, (int) mouseY);
        if (widget != null) {
            // Check if clicking on a resize edge
            DraggableWidget.ResizeEdge edge = widget.getResizeEdge((int) mouseX, (int) mouseY);
            if (edge != null) {
                // Start resizing
                editor.startResizing(widget, (int) mouseX, (int) mouseY, edge);
            } else {
                // Start dragging
                editor.startDragging(widget, (int) mouseX, (int) mouseY);
            }
            cir.setReturnValue(true);
            return;
        }

        // Block other interactions in editor mode
        cir.setReturnValue(true);
    }

    /**
     * Handle mouse release for editor mode.
     */
    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void booster$onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        if (editor.isDragging()) {
            editor.stopDragging();
        }
        if (editor.isResizing()) {
            editor.stopResizing();
        }
    }

    /**
     * Handle mouse drag for editor mode.
     */
    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void booster$onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        if (editor.isEditorModeActive()) {
            if (editor.isDragging()) {
                editor.updateDragging((int) mouseX, (int) mouseY);
                cir.setReturnValue(true);
            } else if (editor.isResizing()) {
                editor.updateResizing((int) mouseX, (int) mouseY);
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Block slot clicks in editor mode.
     */
    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void booster$onSlotClick(CallbackInfo ci) {
        if (EditorModeManager.getInstance().isEditorModeActive()) {
            ci.cancel();
        }
    }

    /**
     * Clean up when screen closes.
     */
    @Inject(method = "close", at = @At("HEAD"))
    private void booster$onClose(CallbackInfo ci) {
        EditorModeManager.getInstance().reset();
        booster$editorSidebar = null;
    }
}
