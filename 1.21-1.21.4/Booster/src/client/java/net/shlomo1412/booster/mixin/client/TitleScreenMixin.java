package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.shlomo1412.booster.client.editor.DraggableWidget;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.editor.ScreenInfo;
import net.shlomo1412.booster.client.editor.widget.ConfigButton;
import net.shlomo1412.booster.client.editor.widget.EditButton;
import net.shlomo1412.booster.client.editor.widget.EditorSidebar;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.LastServerModule;
import net.shlomo1412.booster.client.module.modules.LastWorldModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to inject Booster widgets into the title screen.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique
    private EditButton booster$editButton;
    
    @Unique
    private ConfigButton booster$configButton;
    
    @Unique
    private EditorSidebar booster$editorSidebar;
    
    @Unique
    private LastServerModule booster$lastServerModule;
    
    @Unique
    private LastWorldModule booster$lastWorldModule;
    
    @Unique
    private boolean booster$hasBoosterContent = false;

    protected TitleScreenMixin(net.minecraft.text.Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void booster$onInit(CallbackInfo ci) {
        if (!ModuleManager.getInstance().isInitialized()) {
            return;
        }

        // Reset editor state
        EditorModeManager editor = EditorModeManager.getInstance();
        editor.setCurrentScreen(this);
        editor.clearDraggableWidgets();
        booster$hasBoosterContent = false;
        booster$lastServerModule = null;
        booster$lastWorldModule = null;
        booster$editorSidebar = null;
        
        TitleScreen self = (TitleScreen) (Object) this;
        
        // Calculate anchor position - below the main menu buttons
        int anchorX = this.width / 2 - 100;  // Same as vanilla button X
        int anchorY = this.height / 4 + 48 + 72 + 12;  // Below the main buttons with some padding
        
        // Add Last World button
        booster$lastWorldModule = ModuleManager.getInstance().getModule(LastWorldModule.class);
        if (booster$lastWorldModule != null && booster$lastWorldModule.isEnabled()) {
            booster$hasBoosterContent = true;
            booster$lastWorldModule.createButton(
                self,
                anchorX,
                anchorY,
                button -> this.addDrawableChild(button)
            );
        }
        
        // Add Last Server button (next to Last World)
        booster$lastServerModule = ModuleManager.getInstance().getModule(LastServerModule.class);
        if (booster$lastServerModule != null && booster$lastServerModule.isEnabled()) {
            booster$hasBoosterContent = true;
            booster$lastServerModule.createButton(
                self,
                anchorX + 102,  // Next to Last World button
                anchorY,
                button -> this.addDrawableChild(button)
            );
        }

        // Add Edit and Config buttons at BOTTOM-LEFT corner (away from Minecraft buttons)
        if (booster$hasBoosterContent) {
            int configX = 4;
            int configY = this.height - 24;
            
            booster$configButton = ConfigButton.create(configX, configY, this);
            this.addDrawableChild(booster$configButton);
            
            int editX = configX + 22;
            int editY = configY;
            
            booster$editButton = EditButton.create(editX, editY, this::booster$onEditorToggle);
            this.addDrawableChild(booster$editButton);
        }
    }

    @Unique
    private void booster$onEditorToggle() {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        if (editor.isEditorModeActive()) {
            List<GUIModule> activeModules = new ArrayList<>();
            
            if (booster$lastWorldModule != null) {
                activeModules.add(booster$lastWorldModule);
            }
            if (booster$lastServerModule != null) {
                activeModules.add(booster$lastServerModule);
            }
            
            ScreenInfo screenInfo = new ScreenInfo(this, 0, 0, this.width, this.height);
            booster$editorSidebar = new EditorSidebar(
                MinecraftClient.getInstance(), 
                screenInfo, 
                activeModules,
                element -> {}
            );
            // Register sidebar with EditorModeManager for event delegation
            editor.setCurrentSidebar(booster$editorSidebar);
        } else {
            if (booster$editorSidebar != null) {
                booster$editorSidebar.close();
            }
            // Clear sidebar reference
            editor.clearCurrentSidebar();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void booster$onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        // Render editor sidebar if active
        if (booster$editorSidebar != null) {
            if (booster$editorSidebar.isClosed()) {
                booster$editorSidebar = null;
            } else {
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 600);
                
                int sidebarRightEdge = booster$editorSidebar.getRightEdge();
                if (sidebarRightEdge > 0 && editor.isEditorModeActive()) {
                    context.fill(sidebarRightEdge, 0, this.width, this.height, 0x60000000);
                }
                
                if (editor.isEditorModeActive()) {
                    context.fill(this.width / 2 - 60, 0, this.width / 2 + 60, 20, 0xDD000000);
                    context.fill(this.width / 2 - 60, 20, this.width / 2 + 60, 22, 0xFFFFAA00);
                    context.drawCenteredTextWithShadow(this.textRenderer, 
                            "§6§lEDITOR MODE", this.width / 2, 6, 0xFFFFAA00);
                }
                
                booster$editorSidebar.render(context, mouseX, mouseY, delta);
                
                context.getMatrices().pop();
            }
        }
    }

    // Mouse handling is done through ScreenEditorHandler using Fabric Screen Events API
    // and through the EditorSidebar directly. The sidebar reference is available via
    // ScreenEditorHandler.getSidebar() pattern.
    
    /**
     * @return The current editor sidebar, or null if not active
     */
    @Unique
    public EditorSidebar booster$getSidebar() {
        return booster$editorSidebar;
    }
}
