package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
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
import net.shlomo1412.booster.client.module.modules.AutoArmorModule;
import net.shlomo1412.booster.client.module.modules.ClearFurnaceModule;
import net.shlomo1412.booster.client.module.modules.ClearGridModule;
import net.shlomo1412.booster.client.module.modules.DropAllContainerModule;
import net.shlomo1412.booster.client.module.modules.DropAllModule;
import net.shlomo1412.booster.client.module.modules.EstimatedFuelTimeModule;
import net.shlomo1412.booster.client.module.modules.HighlightFuelModule;
import net.shlomo1412.booster.client.module.modules.InfiniteCraftModule;
import net.shlomo1412.booster.client.module.modules.InventoryProgressModule;
import net.shlomo1412.booster.client.module.modules.PinEstimatedTimeModule;
import net.shlomo1412.booster.client.module.modules.SearchBarModule;
import net.shlomo1412.booster.client.module.modules.SmartFuelModule;
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
    private DropAllModule booster$dropAllModule;
    
    @Unique
    private DropAllContainerModule booster$dropAllContainerModule;
    
    @Unique
    private net.shlomo1412.booster.client.widget.BoosterProgressBar booster$progressBarWidget;
    
    // Crafting screen modules
    @Unique
    private ClearGridModule booster$clearGridModule;
    
    @Unique
    private InfiniteCraftModule booster$infiniteCraftModule;
    
    // Player inventory modules
    @Unique
    private AutoArmorModule booster$autoArmorModule;
    
    // Furnace screen modules
    @Unique
    private EstimatedFuelTimeModule booster$estimatedFuelTimeModule;
    
    @Unique
    private PinEstimatedTimeModule booster$pinEstimatedTimeModule;
    
    @Unique
    private SmartFuelModule booster$smartFuelModule;
    
    @Unique
    private HighlightFuelModule booster$highlightFuelModule;
    
    @Unique
    private ClearFurnaceModule booster$clearFurnaceModule;

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
        booster$dropAllModule = null;
        booster$dropAllContainerModule = null;
        booster$progressBarWidget = null;
        booster$clearGridModule = null;
        booster$infiniteCraftModule = null;
        booster$autoArmorModule = null;
        booster$estimatedFuelTimeModule = null;
        booster$pinEstimatedTimeModule = null;
        booster$smartFuelModule = null;
        booster$highlightFuelModule = null;
        booster$clearFurnaceModule = null;

        // Determine screen type
        boolean isContainerScreen = handler instanceof GenericContainerScreenHandler;
        boolean isPlayerInventory = handler instanceof PlayerScreenHandler;
        boolean isCraftingScreen = handler instanceof CraftingScreenHandler;
        boolean isFurnaceScreen = handler instanceof AbstractFurnaceScreenHandler;
        
        // Only process screens we support
        if (!isContainerScreen && !isPlayerInventory && !isCraftingScreen && !isFurnaceScreen) {
            return;
        }
        
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;

        // Container-only modules (Steal/Store, Search Bar, Inventory Progress, Sort Container)
        if (isContainerScreen) {
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
            
            // Add Sort Container button (sorts container contents) - container only
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
        }
        
        // Crafting table screen modules
        if (isCraftingScreen) {
            // Add Clear Grid button
            booster$clearGridModule = ModuleManager.getInstance().getModule(ClearGridModule.class);
            if (booster$clearGridModule != null) {
                booster$hasBoosterContent = true;
                
                if (booster$clearGridModule.isEnabled()) {
                    booster$clearGridModule.createButton(
                        self,
                        x + backgroundWidth,  // Right edge of crafting GUI
                        y,
                        button -> this.addDrawableChild(button)
                    );
                }
            }
            
            // Add Infinite Craft button
            booster$infiniteCraftModule = ModuleManager.getInstance().getModule(InfiniteCraftModule.class);
            if (booster$infiniteCraftModule != null) {
                booster$hasBoosterContent = true;
                
                if (booster$infiniteCraftModule.isEnabled()) {
                    booster$infiniteCraftModule.createButton(
                        self,
                        x + backgroundWidth,  // Right edge of crafting GUI
                        y,
                        button -> this.addDrawableChild(button)
                    );
                }
            }
        }
        
        // Add Sort Inventory button (works on container, player inventory, and crafting screens)
        booster$sortInventoryModule = ModuleManager.getInstance().getModule(SortInventoryModule.class);
        if (booster$sortInventoryModule != null && (isContainerScreen || isPlayerInventory)) {
            booster$hasBoosterContent = true;
            
            if (booster$sortInventoryModule.isEnabled()) {
                booster$sortInventoryModule.createButton(
                    self,
                    x + backgroundWidth,  // Right edge of container/inventory
                    y,                    // Use consistent anchor (container top)
                    backgroundHeight,     // Pass height so module can calculate inventory section
                    isContainerScreen,    // Different position for container vs inventory
                    button -> this.addDrawableChild(button)
                );
            }
        }
        
        // Add Drop All button (works on container and player inventory screens)
        booster$dropAllModule = ModuleManager.getInstance().getModule(DropAllModule.class);
        if (booster$dropAllModule != null && (isContainerScreen || isPlayerInventory)) {
            booster$hasBoosterContent = true;
            
            if (booster$dropAllModule.isEnabled()) {
                booster$dropAllModule.createButton(
                    self,
                    x + backgroundWidth,  // Right edge of container/inventory
                    y,                    // Use consistent anchor (container top)
                    backgroundHeight,     // Pass height so module can calculate inventory section
                    isContainerScreen,    // Different position for container vs inventory
                    button -> this.addDrawableChild(button)
                );
            }
        }
        
        // Add Drop All Container button (container screen only)
        if (isContainerScreen) {
            booster$dropAllContainerModule = ModuleManager.getInstance().getModule(DropAllContainerModule.class);
            if (booster$dropAllContainerModule != null) {
                booster$hasBoosterContent = true;
                
                if (booster$dropAllContainerModule.isEnabled()) {
                    booster$dropAllContainerModule.createButton(
                        self,
                        x + backgroundWidth,  // Right edge of container
                        y,
                        button -> this.addDrawableChild(button)
                    );
                }
            }
        }
        
        // Add Auto Armor button (player inventory only)
        if (isPlayerInventory) {
            booster$autoArmorModule = ModuleManager.getInstance().getModule(AutoArmorModule.class);
            if (booster$autoArmorModule != null) {
                booster$hasBoosterContent = true;
                
                if (booster$autoArmorModule.isEnabled()) {
                    booster$autoArmorModule.createButton(
                        self,
                        x + backgroundWidth,  // Right edge of inventory
                        y,
                        button -> this.addDrawableChild(button)
                    );
                }
            }
        }
        
        // Add furnace screen modules (works on furnace, blast furnace, smoker)
        if (isFurnaceScreen) {
            int furnaceButtonY = y;
            int furnaceButtonX = x + backgroundWidth;
            
            // Estimated Fuel Time module (just a widget, no button)
            booster$estimatedFuelTimeModule = ModuleManager.getInstance().getModule(EstimatedFuelTimeModule.class);
            if (booster$estimatedFuelTimeModule != null) {
                booster$hasBoosterContent = true;
                
                if (booster$estimatedFuelTimeModule.isEnabled()) {
                    booster$estimatedFuelTimeModule.createWidget(
                        self,
                        furnaceButtonX,
                        furnaceButtonY,
                        button -> this.addDrawableChild(button)
                    );
                }
            }
            
            // Pin Estimated Time module
            booster$pinEstimatedTimeModule = ModuleManager.getInstance().getModule(PinEstimatedTimeModule.class);
            if (booster$pinEstimatedTimeModule != null) {
                booster$hasBoosterContent = true;
                
                if (booster$pinEstimatedTimeModule.isEnabled()) {
                    booster$pinEstimatedTimeModule.createButton(
                        self,
                        furnaceButtonX,
                        furnaceButtonY,
                        button -> this.addDrawableChild(button)
                    );
                    furnaceButtonY += 22;
                }
            }
            
            // Smart Fuel module
            booster$smartFuelModule = ModuleManager.getInstance().getModule(SmartFuelModule.class);
            if (booster$smartFuelModule != null) {
                booster$hasBoosterContent = true;
                
                if (booster$smartFuelModule.isEnabled()) {
                    booster$smartFuelModule.createButton(
                        self,
                        furnaceButtonX,
                        furnaceButtonY,
                        button -> this.addDrawableChild(button)
                    );
                    furnaceButtonY += 22;
                }
            }
            
            // Highlight Fuel module (no button, just slot highlighting)
            booster$highlightFuelModule = ModuleManager.getInstance().getModule(HighlightFuelModule.class);
            if (booster$highlightFuelModule != null) {
                booster$hasBoosterContent = true;
                
                if (booster$highlightFuelModule.isEnabled()) {
                    // Update fuel slots for highlighting
                    booster$highlightFuelModule.updateFuelSlots((AbstractFurnaceScreenHandler) handler);
                }
            }
            
            // Clear Furnace module
            booster$clearFurnaceModule = ModuleManager.getInstance().getModule(ClearFurnaceModule.class);
            if (booster$clearFurnaceModule != null) {
                booster$hasBoosterContent = true;
                
                if (booster$clearFurnaceModule.isEnabled()) {
                    booster$clearFurnaceModule.createButton(
                        self,
                        furnaceButtonX,
                        furnaceButtonY,
                        button -> this.addDrawableChild(button)
                    );
                }
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
            // Determine screen type
            boolean isContainerScreen = handler instanceof GenericContainerScreenHandler;
            boolean isCraftingScreen = handler instanceof CraftingScreenHandler;
            
            // Create sidebar with modules appropriate for this screen type
            List<GUIModule> activeModules = new ArrayList<>();
            
            // Container-only modules
            if (isContainerScreen) {
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
                
                SortContainerModule sortContainer = ModuleManager.getInstance().getModule(SortContainerModule.class);
                if (sortContainer != null) {
                    activeModules.add(sortContainer);
                }
            }
            
            // Crafting table modules
            if (isCraftingScreen) {
                ClearGridModule clearGrid = ModuleManager.getInstance().getModule(ClearGridModule.class);
                if (clearGrid != null) {
                    activeModules.add(clearGrid);
                }
                
                InfiniteCraftModule infiniteCraft = ModuleManager.getInstance().getModule(InfiniteCraftModule.class);
                if (infiniteCraft != null) {
                    activeModules.add(infiniteCraft);
                }
            }
            
            // Sort Inventory works on container and player inventory screens
            if (isContainerScreen || handler instanceof PlayerScreenHandler) {
                SortInventoryModule sortInventory = ModuleManager.getInstance().getModule(SortInventoryModule.class);
                if (sortInventory != null) {
                    activeModules.add(sortInventory);
                }
                
                DropAllModule dropAll = ModuleManager.getInstance().getModule(DropAllModule.class);
                if (dropAll != null) {
                    activeModules.add(dropAll);
                }
            }
            
            // Drop All Container works on container screen only
            if (isContainerScreen) {
                DropAllContainerModule dropAllContainer = ModuleManager.getInstance().getModule(DropAllContainerModule.class);
                if (dropAllContainer != null) {
                    activeModules.add(dropAllContainer);
                }
            }
            
            // Auto Armor works on player inventory screen only
            if (handler instanceof PlayerScreenHandler) {
                AutoArmorModule autoArmor = ModuleManager.getInstance().getModule(AutoArmorModule.class);
                if (autoArmor != null) {
                    activeModules.add(autoArmor);
                }
            }
            
            // Furnace screen modules (furnace, blast furnace, smoker)
            if (handler instanceof AbstractFurnaceScreenHandler) {
                EstimatedFuelTimeModule estimatedFuel = ModuleManager.getInstance().getModule(EstimatedFuelTimeModule.class);
                if (estimatedFuel != null) {
                    activeModules.add(estimatedFuel);
                }
                
                PinEstimatedTimeModule pinEstimated = ModuleManager.getInstance().getModule(PinEstimatedTimeModule.class);
                if (pinEstimated != null) {
                    activeModules.add(pinEstimated);
                }
                
                SmartFuelModule smartFuel = ModuleManager.getInstance().getModule(SmartFuelModule.class);
                if (smartFuel != null) {
                    activeModules.add(smartFuel);
                }
                
                HighlightFuelModule highlightFuel = ModuleManager.getInstance().getModule(HighlightFuelModule.class);
                if (highlightFuel != null) {
                    activeModules.add(highlightFuel);
                }
                
                ClearFurnaceModule clearFurnace = ModuleManager.getInstance().getModule(ClearFurnaceModule.class);
                if (clearFurnace != null) {
                    activeModules.add(clearFurnace);
                }
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
        // Tick infinite craft module (handles continuous crafting)
        if (booster$infiniteCraftModule != null && booster$infiniteCraftModule.isEnabled()) {
            booster$infiniteCraftModule.tick((HandledScreen<?>) (Object) this);
        }
        
        // Tick auto armor module (handles automatic armor equipping)
        if (booster$autoArmorModule != null && booster$autoArmorModule.isEnabled()) {
            booster$autoArmorModule.tick((HandledScreen<?>) (Object) this);
        }
        
        // Render furnace modules (if on furnace screen)
        if (handler instanceof AbstractFurnaceScreenHandler) {
            AbstractFurnaceScreenHandler furnaceHandler = (AbstractFurnaceScreenHandler) handler;
            
            // Update and render highlight fuel module (update every frame to catch inventory changes)
            if (booster$highlightFuelModule != null && booster$highlightFuelModule.isEnabled()) {
                booster$highlightFuelModule.updateFuelSlots(furnaceHandler);
                booster$highlightFuelModule.renderHighlights(context, furnaceHandler, x, y);
            }
            
            // Render estimated fuel time display
            if (booster$estimatedFuelTimeModule != null && booster$estimatedFuelTimeModule.isEnabled()) {
                booster$estimatedFuelTimeModule.renderTime(context, furnaceHandler, x, y);
            }
            
            // Update pinned furnace tracking (to keep time accurate when slots change)
            if (booster$pinEstimatedTimeModule != null && booster$pinEstimatedTimeModule.isEnabled()) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult blockHit) {
                    booster$pinEstimatedTimeModule.updateTrackedFurnace(blockHit.getBlockPos(), furnaceHandler);
                }
            }
        }
        
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
        if (booster$clearGridModule != null) {
            booster$clearGridModule.clearButton();
            booster$clearGridModule = null;
        }
        if (booster$infiniteCraftModule != null) {
            booster$infiniteCraftModule.clearButton();
            booster$infiniteCraftModule = null;
        }
    }
}
