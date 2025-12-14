package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.editor.ScreenInfo;
import net.shlomo1412.booster.client.editor.widget.ConfigButton;
import net.shlomo1412.booster.client.editor.widget.EditButton;
import net.shlomo1412.booster.client.editor.widget.EditorSidebar;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.DeathCoordinatesModule;
import net.shlomo1412.booster.client.module.modules.DeathInventoryModule;
import net.shlomo1412.booster.client.module.modules.RecoverItemsModule;
import net.shlomo1412.booster.client.module.modules.TeleportToDeathModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to inject Booster widgets into the death screen.
 */
@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {

    @Unique
    private EditButton booster$editButton;
    
    @Unique
    private ConfigButton booster$configButton;
    
    @Unique
    private EditorSidebar booster$editorSidebar;
    
    @Unique
    private DeathCoordinatesModule booster$deathCoordsModule;
    
    @Unique
    private TeleportToDeathModule booster$tpDeathModule;
    
    @Unique
    private DeathInventoryModule booster$deathInventoryModule;
    
    @Unique
    private RecoverItemsModule booster$recoverItemsModule;
    
    @Unique
    private boolean booster$hasBoosterContent = false;

    protected DeathScreenMixin(net.minecraft.text.Text title) {
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
        booster$deathCoordsModule = null;
        booster$tpDeathModule = null;
        booster$deathInventoryModule = null;
        booster$recoverItemsModule = null;
        booster$editorSidebar = null;
        
        DeathScreen self = (DeathScreen) (Object) this;
        
        // Anchor position - below the death message and buttons
        int anchorX = this.width / 2 - 100;
        int anchorY = this.height / 4 + 72 + 24;  // Below the respawn button
        
        // Death Coordinates display
        booster$deathCoordsModule = ModuleManager.getInstance().getModule(DeathCoordinatesModule.class);
        if (booster$deathCoordsModule != null && booster$deathCoordsModule.isEnabled()) {
            if (DeathCoordinatesModule.hasDeathLocation()) {
                booster$hasBoosterContent = true;
                booster$deathCoordsModule.createDisplay(
                    self,
                    anchorX,
                    anchorY,
                    button -> this.addDrawableChild(button)
                );
            }
        }
        
        // TP to Death button (next to coordinates)
        booster$tpDeathModule = ModuleManager.getInstance().getModule(TeleportToDeathModule.class);
        if (booster$tpDeathModule != null && booster$tpDeathModule.isEnabled()) {
            if (booster$tpDeathModule.shouldShow() && DeathCoordinatesModule.hasDeathLocation()) {
                booster$hasBoosterContent = true;
                booster$tpDeathModule.createButton(
                    self,
                    anchorX + 155,  // After coordinates
                    anchorY,
                    button -> this.addDrawableChild(button)
                );
            }
        }
        
        // Death Inventory button
        booster$deathInventoryModule = ModuleManager.getInstance().getModule(DeathInventoryModule.class);
        if (booster$deathInventoryModule != null && booster$deathInventoryModule.isEnabled()) {
            if (DeathInventoryModule.hasDeathInventory()) {
                booster$hasBoosterContent = true;
                booster$deathInventoryModule.createButton(
                    self,
                    anchorX + 180,  // After TP button
                    anchorY,
                    button -> this.addDrawableChild(button)
                );
            }
        }
        
        // Recover Items button
        booster$recoverItemsModule = ModuleManager.getInstance().getModule(RecoverItemsModule.class);
        if (booster$recoverItemsModule != null && booster$recoverItemsModule.isEnabled()) {
            if (booster$recoverItemsModule.shouldShow() && DeathInventoryModule.hasDeathInventory()) {
                booster$hasBoosterContent = true;
                booster$recoverItemsModule.createButton(
                    self,
                    anchorX + 205,  // After Inventory button
                    anchorY,
                    button -> this.addDrawableChild(button)
                );
            }
        }

        // Add Edit and Config buttons at BOTTOM-LEFT corner
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
            
            if (booster$deathCoordsModule != null && booster$deathCoordsModule.getButton() != null) {
                activeModules.add(booster$deathCoordsModule);
            }
            if (booster$tpDeathModule != null && booster$tpDeathModule.getButton() != null) {
                activeModules.add(booster$tpDeathModule);
            }
            if (booster$deathInventoryModule != null && booster$deathInventoryModule.getButton() != null) {
                activeModules.add(booster$deathInventoryModule);
            }
            if (booster$recoverItemsModule != null) {
                activeModules.add(booster$recoverItemsModule);
            }
            
            ScreenInfo screenInfo = new ScreenInfo(this, 0, 0, this.width, this.height);
            booster$editorSidebar = new EditorSidebar(
                MinecraftClient.getInstance(), 
                screenInfo, 
                activeModules,
                element -> {}
            );
            editor.setCurrentSidebar(booster$editorSidebar);
        } else {
            if (booster$editorSidebar != null) {
                booster$editorSidebar.close();
            }
            editor.clearCurrentSidebar();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void booster$onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        EditorModeManager editor = EditorModeManager.getInstance();
        
        // Render inventory panel at higher z-level
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 400);
        
        if (booster$deathInventoryModule != null) {
            booster$deathInventoryModule.renderInventoryPanel(context, mouseX, mouseY);
        }
        
        context.getMatrices().pop();
        
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

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Handle inventory panel click
        if (booster$deathInventoryModule != null && booster$deathInventoryModule.isShowingInventory()) {
            booster$deathInventoryModule.handleClick(mouseX, mouseY);
        }
    }

    @Unique
    public EditorSidebar booster$getSidebar() {
        return booster$editorSidebar;
    }
}
