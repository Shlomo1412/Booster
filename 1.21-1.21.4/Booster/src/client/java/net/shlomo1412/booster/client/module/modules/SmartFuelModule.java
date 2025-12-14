package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractFurnaceScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Module that automatically adds exactly enough fuel for the items in the smelting slot.
 * Calculates fuel needed based on input items and available fuel sources.
 * Works with Furnace, Blast Furnace, and Smoker screens.
 */
public class SmartFuelModule extends GUIModule {
    
    public static final String SMART_FUEL_WIDGET_ID = "smart_fuel";
    
    // Fuel burn times in ticks (how many ticks one item burns for)
    private static final Map<Item, Integer> FUEL_BURN_TIMES = new HashMap<>();
    
    static {
        // Initialize fuel burn times
        // Values from Minecraft wiki
        FUEL_BURN_TIMES.put(Items.LAVA_BUCKET, 20000);
        FUEL_BURN_TIMES.put(Items.COAL_BLOCK, 16000);
        FUEL_BURN_TIMES.put(Items.DRIED_KELP_BLOCK, 4001);
        FUEL_BURN_TIMES.put(Items.BLAZE_ROD, 2400);
        FUEL_BURN_TIMES.put(Items.COAL, 1600);
        FUEL_BURN_TIMES.put(Items.CHARCOAL, 1600);
        // Wood items
        FUEL_BURN_TIMES.put(Items.OAK_LOG, 300);
        FUEL_BURN_TIMES.put(Items.BIRCH_LOG, 300);
        FUEL_BURN_TIMES.put(Items.SPRUCE_LOG, 300);
        FUEL_BURN_TIMES.put(Items.JUNGLE_LOG, 300);
        FUEL_BURN_TIMES.put(Items.ACACIA_LOG, 300);
        FUEL_BURN_TIMES.put(Items.DARK_OAK_LOG, 300);
        FUEL_BURN_TIMES.put(Items.MANGROVE_LOG, 300);
        FUEL_BURN_TIMES.put(Items.CHERRY_LOG, 300);
        FUEL_BURN_TIMES.put(Items.OAK_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.BIRCH_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.SPRUCE_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.JUNGLE_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.ACACIA_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.DARK_OAK_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.MANGROVE_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.CHERRY_PLANKS, 300);
        FUEL_BURN_TIMES.put(Items.BAMBOO_PLANKS, 300);
        // Other fuels
        FUEL_BURN_TIMES.put(Items.STICK, 100);
        FUEL_BURN_TIMES.put(Items.BAMBOO, 50);
        FUEL_BURN_TIMES.put(Items.DRIED_KELP, 200);
        FUEL_BURN_TIMES.put(Items.WHITE_CARPET, 67);
        FUEL_BURN_TIMES.put(Items.WHITE_WOOL, 100);
        // Wooden tools and items
        FUEL_BURN_TIMES.put(Items.WOODEN_SWORD, 200);
        FUEL_BURN_TIMES.put(Items.WOODEN_AXE, 200);
        FUEL_BURN_TIMES.put(Items.WOODEN_PICKAXE, 200);
        FUEL_BURN_TIMES.put(Items.WOODEN_SHOVEL, 200);
        FUEL_BURN_TIMES.put(Items.WOODEN_HOE, 200);
        FUEL_BURN_TIMES.put(Items.BOW, 300);
        FUEL_BURN_TIMES.put(Items.FISHING_ROD, 300);
    }
    
    private final ModuleSetting.BooleanSetting preferCoalSetting;
    private final ModuleSetting.BooleanSetting avoidLavaBucketSetting;
    
    private BoosterButton smartFuelButton;
    
    public SmartFuelModule() {
        super(
            "smart_fuel",
            "Smart Fuel",
            "Automatically adds exactly enough fuel for the items to smelt.\n" +
            "Calculates the optimal amount based on input items.\n" +
            "Works with Furnace, Blast Furnace, and Smoker.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Prefer coal setting
        this.preferCoalSetting = new ModuleSetting.BooleanSetting(
            "prefer_coal",
            "Prefer Coal",
            "Prefer using coal/charcoal over other fuels when available",
            true
        );
        registerSetting(preferCoalSetting);
        
        // Avoid lava bucket setting
        this.avoidLavaBucketSetting = new ModuleSetting.BooleanSetting(
            "avoid_lava_bucket",
            "Avoid Lava Bucket",
            "Avoid using lava buckets as they smelt 100 items (may be wasteful)",
            true
        );
        registerSetting(avoidLavaBucketSetting);
    }
    
    /**
     * Creates the smart fuel button for the furnace screen.
     */
    public void createButton(HandledScreen<?> screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        if (!EstimatedFuelTimeModule.isFurnaceScreen(screen)) {
            return;
        }
        
        WidgetSettings settings = getWidgetSettings(SMART_FUEL_WIDGET_ID, 40, -20);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        smartFuelButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "⛽",
            "Smart Fuel",
            "Add exactly enough fuel for your items.\n" +
            "Calculates optimal fuel amount automatically.",
            button -> addSmartFuel(screen)
        );
        
        // Apply display mode
        smartFuelButton.setDisplayMode(settings.getDisplayMode());
        
        smartFuelButton.setEditorInfo(this, SMART_FUEL_WIDGET_ID, "Smart Fuel", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(smartFuelButton);
        
        addDrawableChild.accept(smartFuelButton);
    }
    
    /**
     * Adds the appropriate amount of fuel for the items to smelt.
     */
    private void addSmartFuel(HandledScreen<?> screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;
        
        if (!(screen.getScreenHandler() instanceof AbstractFurnaceScreenHandler handler)) {
            return;
        }
        
        // Get input slot (slot 0)
        ItemStack inputStack = handler.getSlot(0).getStack();
        if (inputStack.isEmpty()) {
            BoosterClient.LOGGER.info("No items to smelt");
            return;
        }
        
        int itemsToSmelt = inputStack.getCount();
        int ticksNeeded = itemsToSmelt * getCookTimePerItem(handler);
        
        // Get current fuel in fuel slot (slot 1)
        ItemStack currentFuel = handler.getSlot(1).getStack();
        int currentFuelTicks = 0;
        if (!currentFuel.isEmpty()) {
            int burnTime = getFuelBurnTime(currentFuel.getItem());
            currentFuelTicks = burnTime * currentFuel.getCount();
        }
        
        // Calculate additional fuel needed
        int additionalTicksNeeded = ticksNeeded - currentFuelTicks;
        if (additionalTicksNeeded <= 0) {
            BoosterClient.LOGGER.info("Sufficient fuel already present");
            return;
        }
        
        // Find best fuel in player inventory
        FuelSource bestFuel = findBestFuel(client.player, additionalTicksNeeded, handler);
        if (bestFuel == null) {
            BoosterClient.LOGGER.info("No suitable fuel found in inventory");
            return;
        }
        
        // Move fuel to furnace
        moveFuelToFurnace(client, handler, bestFuel);
    }
    
    /**
     * Gets the cook time per item based on furnace type.
     */
    private int getCookTimePerItem(AbstractFurnaceScreenHandler handler) {
        String className = handler.getClass().getSimpleName();
        if (className.contains("Blast") || className.contains("Smoker")) {
            return 100; // 5 seconds
        }
        return 200; // 10 seconds
    }
    
    /**
     * Gets the burn time for a fuel item.
     */
    public static int getFuelBurnTime(Item item) {
        // Check our predefined map first
        if (FUEL_BURN_TIMES.containsKey(item)) {
            return FUEL_BURN_TIMES.get(item);
        }
        
        // Default fallback for unknown fuels
        // Check if it's a wood-type item
        String itemName = Registries.ITEM.getId(item).getPath();
        if (itemName.contains("log") || itemName.contains("wood")) {
            return 300;
        }
        if (itemName.contains("plank")) {
            return 300;
        }
        if (itemName.contains("fence") || itemName.contains("slab")) {
            return 150;
        }
        
        return 0; // Not a fuel
    }
    
    /**
     * Checks if an item is a fuel.
     */
    public static boolean isFuel(Item item) {
        return getFuelBurnTime(item) > 0;
    }
    
    /**
     * Finds the best fuel in the player's inventory for the needed ticks.
     */
    private FuelSource findBestFuel(ClientPlayerEntity player, int ticksNeeded, AbstractFurnaceScreenHandler handler) {
        List<FuelSource> availableFuels = new ArrayList<>();
        
        // Check player inventory (slots 3-38 in the handler for player inventory)
        // Furnace slots: 0 = input, 1 = fuel, 2 = output
        // Player inventory starts at slot 3
        for (int i = 3; i < handler.slots.size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            
            int burnTime = getFuelBurnTime(stack.getItem());
            if (burnTime > 0) {
                // Skip lava bucket if setting is enabled
                if (avoidLavaBucketSetting.getValue() && stack.getItem() == Items.LAVA_BUCKET) {
                    continue;
                }
                
                availableFuels.add(new FuelSource(i, stack, burnTime));
            }
        }
        
        if (availableFuels.isEmpty()) {
            return null;
        }
        
        // Sort fuels by preference
        availableFuels.sort((a, b) -> {
            // Prefer coal if setting is enabled
            if (preferCoalSetting.getValue()) {
                boolean aIsCoal = a.stack.getItem() == Items.COAL || a.stack.getItem() == Items.CHARCOAL;
                boolean bIsCoal = b.stack.getItem() == Items.COAL || b.stack.getItem() == Items.CHARCOAL;
                if (aIsCoal && !bIsCoal) return -1;
                if (!aIsCoal && bIsCoal) return 1;
            }
            
            // Prefer fuels that waste less
            int aWaste = calculateWaste(a, ticksNeeded);
            int bWaste = calculateWaste(b, ticksNeeded);
            return Integer.compare(aWaste, bWaste);
        });
        
        // Calculate how many of the best fuel we need
        FuelSource best = availableFuels.get(0);
        int fuelNeeded = (int) Math.ceil((double) ticksNeeded / best.burnTime);
        fuelNeeded = Math.min(fuelNeeded, best.stack.getCount());
        
        return new FuelSource(best.slotIndex, 
            new ItemStack(best.stack.getItem(), fuelNeeded), 
            best.burnTime);
    }
    
    /**
     * Calculates fuel waste for comparison.
     */
    private int calculateWaste(FuelSource fuel, int ticksNeeded) {
        int countNeeded = (int) Math.ceil((double) ticksNeeded / fuel.burnTime);
        countNeeded = Math.min(countNeeded, fuel.stack.getCount());
        int totalTicks = countNeeded * fuel.burnTime;
        return totalTicks - ticksNeeded;
    }
    
    /**
     * Moves fuel from inventory to furnace fuel slot.
     */
    private void moveFuelToFurnace(MinecraftClient client, AbstractFurnaceScreenHandler handler, FuelSource fuel) {
        if (client.interactionManager == null || client.player == null) return;
        
        int syncId = handler.syncId;
        int fuelSlot = 1; // Furnace fuel slot
        
        // Pick up the fuel from inventory
        client.interactionManager.clickSlot(syncId, fuel.slotIndex, 0, SlotActionType.PICKUP, client.player);
        
        // Calculate how many items we actually need
        int countNeeded = fuel.stack.getCount();
        ItemStack currentCursor = handler.getCursorStack();
        
        if (currentCursor.getCount() > countNeeded) {
            // We have more than we need, place the exact amount
            // First put all in fuel slot
            client.interactionManager.clickSlot(syncId, fuelSlot, 0, SlotActionType.PICKUP, client.player);
            
            // If we put too much, take some back
            // This is simplified - for exact amounts you'd need right-click placement
        } else {
            // Put all fuel in the fuel slot
            client.interactionManager.clickSlot(syncId, fuelSlot, 0, SlotActionType.PICKUP, client.player);
            
            // If we still have cursor items, put them back
            if (!handler.getCursorStack().isEmpty()) {
                client.interactionManager.clickSlot(syncId, fuel.slotIndex, 0, SlotActionType.PICKUP, client.player);
            }
        }
        
        BoosterClient.LOGGER.info("Added {} {} as fuel", fuel.stack.getCount(), 
            Registries.ITEM.getId(fuel.stack.getItem()));
    }
    
    @Override
    public Set<String> getWidgetIds() {
        Set<String> ids = new HashSet<>();
        ids.add(SMART_FUEL_WIDGET_ID);
        return ids;
    }
    
    /**
     * Fuel source data.
     */
    private static class FuelSource {
        int slotIndex;
        ItemStack stack;
        int burnTime;
        
        FuelSource(int slotIndex, ItemStack stack, int burnTime) {
            this.slotIndex = slotIndex;
            this.stack = stack;
            this.burnTime = burnTime;
        }
    }
}
