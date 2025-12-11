package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.shlomo1412.booster.client.editor.DraggableWidget;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.editor.ScreenInfo;
import net.shlomo1412.booster.client.editor.widget.ConfigButton;
import net.shlomo1412.booster.client.editor.widget.EditButton;
import net.shlomo1412.booster.client.editor.widget.EditorSidebar;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.CopyIPModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to inject Booster widgets into the multiplayer/server selection screen.
 */
@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

    @Shadow
    protected MultiplayerServerListWidget serverListWidget;

    @Unique
    private EditButton booster$editButton;
    
    @Unique
    private ConfigButton booster$configButton;
    
    @Unique
    private EditorSidebar booster$editorSidebar;
    
    @Unique
    private CopyIPModule booster$copyIPModule;
    
    @Unique
    private boolean booster$hasBoosterContent = false;

    protected MultiplayerScreenMixin(net.minecraft.text.Text title) {
        super(title);
    }
    
    @Unique
    private ServerInfo booster$getSelectedServer() {
        if (serverListWidget == null) {
            return null;
        }
        MultiplayerServerListWidget.Entry entry = serverListWidget.getSelectedOrNull();
        if (entry instanceof MultiplayerServerListWidget.ServerEntry serverEntry) {
            return serverEntry.getServer();
        }
        return null;
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
        booster$copyIPModule = null;
        booster$editorSidebar = null;
        
        MultiplayerScreen self = (MultiplayerScreen) (Object) this;
        
        // Anchor near the bottom buttons
        int anchorX = this.width / 2 + 4 + 76 + 4;  // After "Join Server" button
        int anchorY = this.height - 52;  // Same row as buttons
        
        // Add Copy IP button
        booster$copyIPModule = ModuleManager.getInstance().getModule(CopyIPModule.class);
        if (booster$copyIPModule != null && booster$copyIPModule.isEnabled()) {
            booster$hasBoosterContent = true;
            booster$copyIPModule.createButton(
                self,
                anchorX,
                anchorY,
                this::booster$getSelectedServer,
                button -> this.addDrawableChild(button)
            );
        }

        // Add Edit and Config buttons at BOTTOM-LEFT corner (below server list)
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
            
            if (booster$copyIPModule != null) {
                activeModules.add(booster$copyIPModule);
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

    /**
     * @return The current editor sidebar, or null if not active
     */
    @Unique
    public EditorSidebar booster$getSidebar() {
        return booster$editorSidebar;
    }
}
