package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.shlomo1412.booster.client.editor.DraggableWidget;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.editor.ScreenInfo;
import net.shlomo1412.booster.client.editor.widget.ConfigButton;
import net.shlomo1412.booster.client.editor.widget.EditButton;
import net.shlomo1412.booster.client.editor.widget.EditorGuide;
import net.shlomo1412.booster.client.editor.widget.EditorSidebar;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.InventoryProgressModule;
import net.shlomo1412.booster.client.module.modules.SearchBarModule;
import net.shlomo1412.booster.client.module.modules.SortContainerModule;
import net.shlomo1412.booster.client.module.modules.SortInventoryModule;
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
    private EditorGuide booster$editorGuide;
    
    @Unique
    private boolean booster$hasBoosterContent = false;
    
    // Module references for rendering
    @Unique
    private SearchBarModule booster$searchBarModule;
    
    @Unique
    private InventoryProgressModule booster$inventoryProgressModule;
    
    @Unique
    private SortInventoryModule booster$sortInventoryModule;
    
    @Unique
    private SortContainerModule booster$sortContainerModule;
    
    @Unique
    private net.shlomo1412.booster.client.widget.BoosterProgressBar booster$progressBarWidget;

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
        booster$searchBarModule = null;
        booster$inventoryProgressModule = null;
        booster$sortInventoryModule = null;
        booster$sortContainerModule = null;
        booster$progressBarWidget = null;

        // Only add Booster content to container screens
        if (!(handler instanceof GenericContainerScreenHandler)) {
            return;
        }
        
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;

        // Add Steal/Store buttons
        StealStoreModule stealStoreModule = ModuleManager.getInstance().getModule(StealStoreModule.class);
        if (stealStoreModule != null) {
            booster$hasBoosterContent = true;
            
            if (stealStoreModule.isEnabled()) {
                // Pass right edge of container as anchor for buttons
                stealStoreModule.createButtons(
                    self,
                    x + backgroundWidth,  // Right edge of container
                    y,
                    button -> this.addDrawableChild(button)
                );
            }
        }
        
        // Add Search Bar
        booster$searchBarModule = ModuleManager.getInstance().getModule(SearchBarModule.class);
        if (booster$searchBarModule != null) {
            booster$hasBoosterContent = true;
            
            if (booster$searchBarModule.isEnabled()) {
                booster$searchBarModule.createSearchBar(
                    self,
                    x,  // Left edge of container
                    y,
                    backgroundWidth,  // Container width for smart positioning
                    this.height,  // Pass screen height for clamping
                    field -> this.addDrawableChild(field)
                );
            }
        }
        
        // Create Inventory Progress Bar widget
        booster$inventoryProgressModule = ModuleManager.getInstance().getModule(InventoryProgressModule.class);
        if (booster$inventoryProgressModule != null) {
            booster$hasBoosterContent = true;
            
            if (booster$inventoryProgressModule.isEnabled()) {
                booster$progressBarWidget = booster$inventoryProgressModule.createProgressBar(self, x, y);
                // Note: We don't add it as a drawable child since it's rendered separately
            }
        }
        
        // Add Sort Inventory button (sorts player inventory)
        booster$sortInventoryModule = ModuleManager.getInstance().getModule(SortInventoryModule.class);
        if (booster$sortInventoryModule != null) {
            booster$hasBoosterContent = true;
            
            if (booster$sortInventoryModule.isEnabled()) {
                booster$sortInventoryModule.createButton(
                    self,
                    x + backgroundWidth,  // Right edge of container
                    y,
                    true,  // isContainerScreen = true
                    button -> this.addDrawableChild(button)
                );
            }
        }
        
        // Add Sort Container button (sorts container contents)
        booster$sortContainerModule = ModuleManager.getInstance().getModule(SortContainerModule.class);
        if (booster$sortContainerModule != null) {
            booster$hasBoosterContent = true;
            
            if (booster$sortContainerModule.isEnabled()) {
                booster$sortContainerModule.createButton(
                    self,
                    x + backgroundWidth,  // Right edge of container
                    y,
                    button -> this.addDrawableChild(button)
                );
            }
        }

        // Add Edit and Config buttons at TOP-RIGHT of SCREEN (not container)
        if (booster$hasBoosterContent) {
            // Position at top-right corner of screen with padding
            int configX = this.width - 46;  // 20 + 2 + 20 + 4 = 46 from right edge
            int configY = 4;
            
            booster$configButton = ConfigButton.create(configX, configY, this);
            this.addDrawableChild(booster$configButton);
            
            // Position edit button next to config button
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
            // Create sidebar with all GUI modules
            List<GUIModule> activeModules = new ArrayList<>();
            
            StealStoreModule stealStore = ModuleManager.getInstance().getModule(StealStoreModule.class);
            if (stealStore != null) {
                activeModules.add(stealStore);
            }
            
            SearchBarModule searchBar = ModuleManager.getInstance().getModule(SearchBarModule.class);
            if (searchBar != null) {
                activeModules.add(searchBar);
            }
            
            InventoryProgressModule inventoryProgress = ModuleManager.getInstance().getModule(InventoryProgressModule.class);
            if (inventoryProgress != null) {
                activeModules.add(inventoryProgress);
            }
            
            SortInventoryModule sortInventory = ModuleManager.getInstance().getModule(SortInventoryModule.class);
            if (sortInventory != null) {
                activeModules.add(sortInventory);
            }
            
            SortContainerModule sortContainer = ModuleManager.getInstance().getModule(SortContainerModule.class);
            if (sortContainer != null) {
                activeModules.add(sortContainer);
            }
            
            ScreenInfo screenInfo = new ScreenInfo(this, x, y, backgroundWidth, backgroundHeight);
            booster$editorSidebar = new EditorSidebar(
                MinecraftClient.getInstance(), 
                screenInfo, 
                activeModules,
                element -> {}
            );
            
            // Show first-time guide if not shown before
            if (!ModuleManager.getInstance().getConfig().hasEditorGuideBeenShown()) {
                booster$editorGuide = new EditorGuide(
                    MinecraftClient.getInstance(),
                    () -> {
                        booster$editorGuide = null;
                        ModuleManager.getInstance().getConfig().markEditorGuideShown();
                        ModuleManager.getInstance().saveConfig();
                    }
                );
            }
        } else {
            if (booster$editorSidebar != null) {
                booster$editorSidebar.close();
            }
            if (booster$editorGuide != null) {
                booster$editorGuide.close();
            }
        }
    }

    /**
     * Render search highlights and progress bar after slots are rendered.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void booster$onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Render inventory progress bar widget (if enabled)
        if (booster$progressBarWidget != null && booster$inventoryProgressModule != null && booster$inventoryProgressModule.isEnabled()) {
            booster$progressBarWidget.render(context, mouseX, mouseY, delta);
        }
        
        // Render search highlights (if search is active)
        if (booster$searchBarModule != null && booster$searchBarModule.isEnabled() && booster$searchBarModule.isSearchActive()) {
            // First dim non-matching items
            booster$searchBarModule.renderSlotDimming(context, x, y);
            // Then highlight matching items
            booster$searchBarModule.renderHighlights(context, x, y);
            
            // Show match count near search bar (only if there's enough space)
            if (booster$searchBarModule.shouldShowMatchCount()) {
                var searchField = booster$searchBarModule.getSearchField();
                if (searchField != null) {
                    int matchCount = booster$searchBarModule.getMatchCount();
                    String matchText = matchCount + " match" + (matchCount != 1 ? "es" : "");
                    int textX = searchField.getX() + searchField.getWidth() + 4;
                    int textY = searchField.getY() + 5;
                    context.drawTextWithShadow(this.textRenderer, matchText, textX, textY, 
                        matchCount > 0 ? 0xFF44FF44 : 0xFFFF4444);
                }
            }
        }
        
        EditorModeManager editor = EditorModeManager.getInstance();
        
        // Render editor UI on top of everything using elevated z-level
        // Items render at z=150-200, item count labels at z=200-250, tooltips at z=400
        // We use z=500 for our overlays to ensure they're above item labels
        if (booster$editorSidebar != null) {
            // Check if sidebar should be removed after close animation
            if (booster$editorSidebar.isClosed()) {
                booster$editorSidebar = null;
            } else {
                // Push matrices and translate to higher z-level
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 600);
                
                // Dim the area to the RIGHT of the sidebar (since sidebar is on left)
                int sidebarRightEdge = booster$editorSidebar.getRightEdge();
                if (sidebarRightEdge > 0 && editor.isEditorModeActive()) {
                    context.fill(sidebarRightEdge, 0, this.width, this.height, 0x60000000);
                }
                
                // Draw "EDITOR MODE" indicator at top center
                if (editor.isEditorModeActive()) {
                    // Background bar for the indicator
                    context.fill(this.width / 2 - 60, 0, this.width / 2 + 60, 20, 0xDD000000);
                    context.fill(this.width / 2 - 60, 20, this.width / 2 + 60, 22, 0xFFFFAA00);
                    context.drawCenteredTextWithShadow(this.textRenderer, 
                            "§6§lEDITOR MODE", this.width / 2, 6, 0xFFFFAA00);
                }
                
                // Render sidebar at elevated z-level
                booster$editorSidebar.render(context, mouseX, mouseY, delta);
                
                context.getMatrices().pop();
            }
        }
        
        // Render guide on top of everything (even higher z-level)
        if (booster$editorGuide != null) {
            if (booster$editorGuide.isClosed()) {
                booster$editorGuide = null;
            } else {
                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 650);
                booster$editorGuide.render(context, mouseX, mouseY, delta);
                context.getMatrices().pop();
            }
        }
    }

    /**
     * Handle mouse scroll for sidebar and sort buttons.
     */
    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void booster$onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        // Handle sidebar scroll
        if (booster$editorSidebar != null && booster$editorSidebar.isMouseOver(mouseX, mouseY)) {
            if (booster$editorSidebar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                cir.setReturnValue(true);
                return;
            }
        }
        
        // Handle sort button scroll (Alt+Scroll to change mode)
        if (booster$sortInventoryModule != null && booster$sortInventoryModule.isEnabled()) {
            if (booster$sortInventoryModule.handleScroll(mouseX, mouseY, verticalAmount)) {
                cir.setReturnValue(true);
                return;
            }
        }
        if (booster$sortContainerModule != null && booster$sortContainerModule.isEnabled()) {
            if (booster$sortContainerModule.handleScroll(mouseX, mouseY, verticalAmount)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    /**
     * Handle mouse clicks for editor mode.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void booster$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // Handle guide clicks first (blocks all other input)
        if (booster$editorGuide != null && !booster$editorGuide.isClosed()) {
            if (booster$editorGuide.mouseClicked(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        }
        
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
        booster$searchBarModule = null;
        booster$inventoryProgressModule = null;
        if (booster$sortInventoryModule != null) {
            booster$sortInventoryModule.clearButton();
            booster$sortInventoryModule = null;
        }
        if (booster$sortContainerModule != null) {
            booster$sortContainerModule.clearButton();
            booster$sortContainerModule = null;
        }
    }
}
