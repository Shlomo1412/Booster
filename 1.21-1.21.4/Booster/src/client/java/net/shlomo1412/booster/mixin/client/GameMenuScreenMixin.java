package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.shlomo1412.booster.client.editor.DraggableWidget;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.editor.ScreenInfo;
import net.shlomo1412.booster.client.editor.widget.ConfigButton;
import net.shlomo1412.booster.client.editor.widget.EditButton;
import net.shlomo1412.booster.client.editor.widget.EditorSidebar;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.ConnectToServerModule;
import net.shlomo1412.booster.client.module.modules.DatapacksFolderModule;
import net.shlomo1412.booster.client.module.modules.OpenScreenshotsModule;
import net.shlomo1412.booster.client.module.modules.OpenWorldFolderModule;
import net.shlomo1412.booster.client.module.modules.ReconnectModule;
import net.shlomo1412.booster.client.module.modules.SaveQuitGameModule;
import net.shlomo1412.booster.client.module.modules.SaveQuitToServersModule;
import net.shlomo1412.booster.client.module.modules.SaveQuitToWorldsModule;
import net.shlomo1412.booster.client.module.modules.ServerInfoModule;
import net.shlomo1412.booster.client.module.modules.SwitchWorldModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to inject Booster widgets into the game menu (pause menu).
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    @Unique
    private EditButton booster$editButton;
    
    @Unique
    private ConfigButton booster$configButton;
    
    @Unique
    private EditorSidebar booster$editorSidebar;
    
    // Common pause menu modules
    @Unique
    private SaveQuitGameModule booster$saveQuitGameModule;
    
    @Unique
    private OpenScreenshotsModule booster$openScreenshotsModule;
    
    // Singleplayer-only modules
    @Unique
    private SaveQuitToWorldsModule booster$saveQuitToWorldsModule;
    
    @Unique
    private OpenWorldFolderModule booster$openWorldFolderModule;
    
    @Unique
    private DatapacksFolderModule booster$datapacksFolderModule;
    
    @Unique
    private SwitchWorldModule booster$switchWorldModule;
    
    // Multiplayer-only modules
    @Unique
    private SaveQuitToServersModule booster$saveQuitToServersModule;
    
    @Unique
    private ReconnectModule booster$reconnectModule;
    
    @Unique
    private ServerInfoModule booster$serverInfoModule;
    
    @Unique
    private ConnectToServerModule booster$connectToServerModule;
    
    @Unique
    private boolean booster$hasBoosterContent = false;

    protected GameMenuScreenMixin(net.minecraft.text.Text title) {
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
        
        // Reset all module references
        booster$saveQuitGameModule = null;
        booster$openScreenshotsModule = null;
        booster$saveQuitToWorldsModule = null;
        booster$saveQuitToServersModule = null;
        booster$openWorldFolderModule = null;
        booster$datapacksFolderModule = null;
        booster$switchWorldModule = null;
        booster$reconnectModule = null;
        booster$serverInfoModule = null;
        booster$connectToServerModule = null;
        booster$editorSidebar = null;
        
        GameMenuScreen self = (GameMenuScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        boolean isSingleplayer = client.isInSingleplayer();
        
        // Anchor position - below the main menu buttons
        int anchorX = this.width / 2 - 102;
        int anchorY = this.height / 4 + 120 + 12;  // Below the standard buttons
        
        // Row 1: Save & Quit Game + Save & Quit to Worlds/Servers
        // Add Save & Quit Game button (works for both SP and MP)
        booster$saveQuitGameModule = ModuleManager.getInstance().getModule(SaveQuitGameModule.class);
        if (booster$saveQuitGameModule != null && booster$saveQuitGameModule.isEnabled()) {
            booster$hasBoosterContent = true;
            booster$saveQuitGameModule.createButton(
                self,
                anchorX,
                anchorY,
                button -> this.addDrawableChild(button)
            );
        }
        
        // Add Save & Quit to Worlds button (singleplayer only)
        if (isSingleplayer) {
            booster$saveQuitToWorldsModule = ModuleManager.getInstance().getModule(SaveQuitToWorldsModule.class);
            if (booster$saveQuitToWorldsModule != null && booster$saveQuitToWorldsModule.isEnabled()) {
                booster$hasBoosterContent = true;
                booster$saveQuitToWorldsModule.createButton(
                    self,
                    anchorX + 122,  // Next to Save & Quit Game
                    anchorY,
                    button -> this.addDrawableChild(button)
                );
            }
        }
        
        // Add Quit to Servers button (multiplayer only)
        if (!isSingleplayer) {
            booster$saveQuitToServersModule = ModuleManager.getInstance().getModule(SaveQuitToServersModule.class);
            if (booster$saveQuitToServersModule != null && booster$saveQuitToServersModule.isEnabled()) {
                booster$hasBoosterContent = true;
                booster$saveQuitToServersModule.createButton(
                    self,
                    anchorX + 122,  // Next to Save & Quit Game
                    anchorY,
                    button -> this.addDrawableChild(button)
                );
            }
        }
        
        // Row 2: Screenshots + World/Server specific buttons
        // Add Open Screenshots button (works for both SP and MP)
        booster$openScreenshotsModule = ModuleManager.getInstance().getModule(OpenScreenshotsModule.class);
        if (booster$openScreenshotsModule != null && booster$openScreenshotsModule.isEnabled()) {
            booster$hasBoosterContent = true;
            booster$openScreenshotsModule.createButton(
                self,
                anchorX,
                anchorY + 24,  // Below the first row of buttons
                button -> this.addDrawableChild(button)
            );
        }
        
        // Singleplayer-only modules: Open World Folder, Datapacks, Switch World
        if (isSingleplayer) {
            int spButtonX = anchorX + 122;
            
            // Open World Folder button
            booster$openWorldFolderModule = ModuleManager.getInstance().getModule(OpenWorldFolderModule.class);
            if (booster$openWorldFolderModule != null && booster$openWorldFolderModule.isEnabled()) {
                booster$hasBoosterContent = true;
                booster$openWorldFolderModule.createButton(
                    self,
                    spButtonX,
                    anchorY + 24,
                    button -> this.addDrawableChild(button)
                );
                spButtonX += 22;
            }
            
            // Datapacks Folder button
            booster$datapacksFolderModule = ModuleManager.getInstance().getModule(DatapacksFolderModule.class);
            if (booster$datapacksFolderModule != null && booster$datapacksFolderModule.isEnabled()) {
                booster$hasBoosterContent = true;
                booster$datapacksFolderModule.createButton(
                    self,
                    spButtonX,
                    anchorY + 24,
                    button -> this.addDrawableChild(button)
                );
                spButtonX += 22;
            }
            
            // Switch World button (with dropdown)
            booster$switchWorldModule = ModuleManager.getInstance().getModule(SwitchWorldModule.class);
            if (booster$switchWorldModule != null && booster$switchWorldModule.isEnabled()) {
                booster$hasBoosterContent = true;
                booster$switchWorldModule.createButton(
                    self,
                    spButtonX,
                    anchorY + 24,
                    button -> this.addDrawableChild(button)
                );
            }
        }
        
        // Multiplayer-only modules: Reconnect, Server Info, Connect to Server
        if (!isSingleplayer) {
            int mpButtonX = anchorX + 122;
            
            // Reconnect button
            booster$reconnectModule = ModuleManager.getInstance().getModule(ReconnectModule.class);
            if (booster$reconnectModule != null && booster$reconnectModule.isEnabled()) {
                booster$hasBoosterContent = true;
                booster$reconnectModule.createButton(
                    self,
                    mpButtonX,
                    anchorY + 24,
                    button -> this.addDrawableChild(button)
                );
                mpButtonX += 22;
            }
            
            // Server Info button (with tooltip)
            booster$serverInfoModule = ModuleManager.getInstance().getModule(ServerInfoModule.class);
            if (booster$serverInfoModule != null && booster$serverInfoModule.isEnabled()) {
                booster$hasBoosterContent = true;
                booster$serverInfoModule.createButton(
                    self,
                    mpButtonX,
                    anchorY + 24,
                    button -> this.addDrawableChild(button)
                );
                mpButtonX += 22;
            }
            
            // Connect to Server button (with dropdown)
            booster$connectToServerModule = ModuleManager.getInstance().getModule(ConnectToServerModule.class);
            if (booster$connectToServerModule != null && booster$connectToServerModule.isEnabled()) {
                booster$hasBoosterContent = true;
                booster$connectToServerModule.createButton(
                    self,
                    mpButtonX,
                    anchorY + 24,
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
            
            // Common modules
            if (booster$saveQuitGameModule != null) {
                activeModules.add(booster$saveQuitGameModule);
            }
            if (booster$openScreenshotsModule != null) {
                activeModules.add(booster$openScreenshotsModule);
            }
            
            // Singleplayer modules
            if (booster$saveQuitToWorldsModule != null) {
                activeModules.add(booster$saveQuitToWorldsModule);
            }
            if (booster$openWorldFolderModule != null) {
                activeModules.add(booster$openWorldFolderModule);
            }
            if (booster$datapacksFolderModule != null) {
                activeModules.add(booster$datapacksFolderModule);
            }
            if (booster$switchWorldModule != null) {
                activeModules.add(booster$switchWorldModule);
            }
            
            // Multiplayer modules
            if (booster$saveQuitToServersModule != null) {
                activeModules.add(booster$saveQuitToServersModule);
            }
            if (booster$reconnectModule != null) {
                activeModules.add(booster$reconnectModule);
            }
            if (booster$serverInfoModule != null) {
                activeModules.add(booster$serverInfoModule);
            }
            if (booster$connectToServerModule != null) {
                activeModules.add(booster$connectToServerModule);
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
        
        // Render dropdowns and panels at higher z-level to appear above buttons
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 400);
        
        // Render dropdowns for modules with dropdown UI
        if (booster$switchWorldModule != null) {
            booster$switchWorldModule.renderDropdown(context, mouseX, mouseY);
        }
        if (booster$connectToServerModule != null) {
            booster$connectToServerModule.renderDropdown(context, mouseX, mouseY);
        }
        
        // Render server info panel
        if (booster$serverInfoModule != null) {
            booster$serverInfoModule.renderInfoPanel(context, mouseX, mouseY);
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

    /**
     * @return The current editor sidebar, or null if not active
     */
    @Unique
    public EditorSidebar booster$getSidebar() {
        return booster$editorSidebar;
    }
    
    /**
     * @return The switch world module, or null if not active
     */
    @Unique
    public SwitchWorldModule booster$getSwitchWorldModule() {
        return booster$switchWorldModule;
    }
    
    /**
     * @return The connect to server module, or null if not active
     */
    @Unique
    public ConnectToServerModule booster$getConnectToServerModule() {
        return booster$connectToServerModule;
    }
}
