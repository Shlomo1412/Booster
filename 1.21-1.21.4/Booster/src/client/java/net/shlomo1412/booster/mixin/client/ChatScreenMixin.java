package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.shlomo1412.booster.client.editor.DraggableWidget;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.editor.ScreenInfo;
import net.shlomo1412.booster.client.editor.widget.ConfigButton;
import net.shlomo1412.booster.client.editor.widget.EditButton;
import net.shlomo1412.booster.client.editor.widget.EditorSidebar;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.AddCoordsModule;
import net.shlomo1412.booster.client.module.modules.ClearChatHistoryModule;
import net.shlomo1412.booster.client.module.modules.ClearChatboxModule;
import net.shlomo1412.booster.client.module.modules.SearchMessagesModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to inject Booster widgets into the chat screen.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    @Shadow
    protected TextFieldWidget chatField;

    // Editor mode components
    @Unique
    private EditButton booster$editButton;
    
    @Unique
    private ConfigButton booster$configButton;
    
    @Unique
    private EditorSidebar booster$editorSidebar;
    
    @Unique
    private boolean booster$hasBoosterContent = false;
    
    // Module references
    @Unique
    private ClearChatboxModule booster$clearChatboxModule;
    
    @Unique
    private ClearChatHistoryModule booster$clearChatHistoryModule;
    
    @Unique
    private AddCoordsModule booster$addCoordsModule;
    
    @Unique
    private SearchMessagesModule booster$searchMessagesModule;

    protected ChatScreenMixin(net.minecraft.text.Text title) {
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
        booster$clearChatboxModule = null;
        booster$clearChatHistoryModule = null;
        booster$addCoordsModule = null;
        booster$searchMessagesModule = null;
        booster$editorSidebar = null;
        
        ChatScreen self = (ChatScreen) (Object) this;
        
        // Anchor position - to the right of the chat input field
        // Chat field is typically at y = height - 12 with height 12
        int chatInputY = this.height - 14;
        int anchorX = 4;  // Chat input usually starts at x=4
        int anchorY = chatInputY;
        
        // Calculate button row Y - above the chat input
        int buttonY = chatInputY - 22;
        
        // Add Clear Chatbox button
        booster$clearChatboxModule = ModuleManager.getInstance().getModule(ClearChatboxModule.class);
        if (booster$clearChatboxModule != null && booster$clearChatboxModule.isEnabled()) {
            booster$hasBoosterContent = true;
            booster$clearChatboxModule.createButton(
                self,
                anchorX,
                buttonY,
                () -> {
                    if (chatField != null) {
                        chatField.setText("");
                    }
                },
                button -> this.addDrawableChild(button)
            );
        }
        
        // Add Clear Chat History button
        booster$clearChatHistoryModule = ModuleManager.getInstance().getModule(ClearChatHistoryModule.class);
        if (booster$clearChatHistoryModule != null && booster$clearChatHistoryModule.isEnabled()) {
            booster$hasBoosterContent = true;
            booster$clearChatHistoryModule.createButton(
                self,
                anchorX,
                buttonY,
                button -> this.addDrawableChild(button)
            );
        }
        
        // Add Coords button
        booster$addCoordsModule = ModuleManager.getInstance().getModule(AddCoordsModule.class);
        if (booster$addCoordsModule != null && booster$addCoordsModule.isEnabled()) {
            booster$hasBoosterContent = true;
            booster$addCoordsModule.createButton(
                self,
                anchorX,
                buttonY,
                text -> {
                    if (chatField != null) {
                        // Insert text at cursor position
                        String current = chatField.getText();
                        int cursor = chatField.getCursor();
                        String newText = current.substring(0, cursor) + text + current.substring(cursor);
                        chatField.setText(newText);
                        chatField.setCursor(cursor + text.length(), false);
                    }
                },
                button -> this.addDrawableChild(button)
            );
        }
        
        // Add Search Messages field
        booster$searchMessagesModule = ModuleManager.getInstance().getModule(SearchMessagesModule.class);
        if (booster$searchMessagesModule != null && booster$searchMessagesModule.isEnabled()) {
            booster$hasBoosterContent = true;
            booster$searchMessagesModule.createSearchField(
                self,
                anchorX,
                buttonY,
                this.width,
                field -> this.addDrawableChild(field)
            );
        }

        // Add Edit and Config buttons at BOTTOM-RIGHT corner (above chat)
        if (booster$hasBoosterContent) {
            int configX = this.width - 46;
            int configY = buttonY;
            
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
            
            if (booster$clearChatboxModule != null) {
                activeModules.add(booster$clearChatboxModule);
            }
            if (booster$clearChatHistoryModule != null) {
                activeModules.add(booster$clearChatHistoryModule);
            }
            if (booster$addCoordsModule != null) {
                activeModules.add(booster$addCoordsModule);
            }
            if (booster$searchMessagesModule != null) {
                activeModules.add(booster$searchMessagesModule);
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
        
        // Render search status if search module is active
        if (booster$searchMessagesModule != null && booster$searchMessagesModule.isEnabled()) {
            booster$searchMessagesModule.renderSearchStatus(context, 0, 0);
        }
        
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
     * @return The search messages module, or null if not active
     */
    @Unique
    public SearchMessagesModule booster$getSearchMessagesModule() {
        return booster$searchMessagesModule;
    }
}
