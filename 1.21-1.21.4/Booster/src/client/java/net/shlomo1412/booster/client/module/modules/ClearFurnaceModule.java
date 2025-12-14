package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Module that clears all items from the furnace to the player's inventory.
 * Moves input, fuel, and output items back to inventory.
 * Works with Furnace, Blast Furnace, and Smoker screens.
 */
public class ClearFurnaceModule extends GUIModule {
    
    public static final String CLEAR_FURNACE_WIDGET_ID = "clear_furnace";
    
    private final ModuleSetting.BooleanSetting clearInputSetting;
    private final ModuleSetting.BooleanSetting clearFuelSetting;
    private final ModuleSetting.BooleanSetting clearOutputSetting;
    
    private BoosterButton clearButton;
    
    public ClearFurnaceModule() {
        super(
            "clear_furnace",
            "Clear Furnace",
            "Clears all items from the furnace to your inventory.\n" +
            "Moves input, fuel, and output items.\n" +
            "Works with Furnace, Blast Furnace, and Smoker.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Clear input setting
        this.clearInputSetting = new ModuleSetting.BooleanSetting(
            "clear_input",
            "Clear Input",
            "Move items from the input slot to inventory",
            true
        );
        registerSetting(clearInputSetting);
        
        // Clear fuel setting
        this.clearFuelSetting = new ModuleSetting.BooleanSetting(
            "clear_fuel",
            "Clear Fuel",
            "Move items from the fuel slot to inventory",
            true
        );
        registerSetting(clearFuelSetting);
        
        // Clear output setting
        this.clearOutputSetting = new ModuleSetting.BooleanSetting(
            "clear_output",
            "Clear Output",
            "Move items from the output slot to inventory",
            true
        );
        registerSetting(clearOutputSetting);
    }
    
    /**
     * Creates the clear button for the furnace screen.
     */
    public void createButton(HandledScreen<?> screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        if (!EstimatedFuelTimeModule.isFurnaceScreen(screen)) {
            return;
        }
        
        WidgetSettings settings = getWidgetSettings(CLEAR_FURNACE_WIDGET_ID, 60, -20);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        clearButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🗑",
            "Clear",
            "Move all items from furnace to your inventory.",
            button -> clearFurnace(screen)
        );
        
        // Apply display mode
        clearButton.setDisplayMode(settings.getDisplayMode());
        
        clearButton.setEditorInfo(this, CLEAR_FURNACE_WIDGET_ID, "Clear Furnace", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(clearButton);
        
        addDrawableChild.accept(clearButton);
    }
    
    /**
     * Clears all items from the furnace.
     */
    private void clearFurnace(HandledScreen<?> screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        if (!(screen.getScreenHandler() instanceof AbstractFurnaceScreenHandler handler)) {
            return;
        }
        
        int syncId = handler.syncId;
        int itemsMoved = 0;
        
        // Furnace slots:
        // 0 = input slot
        // 1 = fuel slot
        // 2 = output slot
        
        // Clear output first (most valuable - smelted items)
        if (clearOutputSetting.getValue()) {
            ItemStack outputStack = handler.getSlot(2).getStack();
            if (!outputStack.isEmpty()) {
                // Shift-click to move to inventory
                client.interactionManager.clickSlot(syncId, 2, 0, SlotActionType.QUICK_MOVE, client.player);
                itemsMoved++;
            }
        }
        
        // Clear input slot
        if (clearInputSetting.getValue()) {
            ItemStack inputStack = handler.getSlot(0).getStack();
            if (!inputStack.isEmpty()) {
                client.interactionManager.clickSlot(syncId, 0, 0, SlotActionType.QUICK_MOVE, client.player);
                itemsMoved++;
            }
        }
        
        // Clear fuel slot
        if (clearFuelSetting.getValue()) {
            ItemStack fuelStack = handler.getSlot(1).getStack();
            if (!fuelStack.isEmpty()) {
                client.interactionManager.clickSlot(syncId, 1, 0, SlotActionType.QUICK_MOVE, client.player);
                itemsMoved++;
            }
        }
        
        if (itemsMoved > 0) {
            BoosterClient.LOGGER.info("Cleared {} item stacks from furnace", itemsMoved);
        } else {
            BoosterClient.LOGGER.info("Furnace is already empty");
        }
    }
    
    @Override
    public Set<String> getWidgetIds() {
        Set<String> ids = new HashSet<>();
        ids.add(CLEAR_FURNACE_WIDGET_ID);
        return ids;
    }
}
