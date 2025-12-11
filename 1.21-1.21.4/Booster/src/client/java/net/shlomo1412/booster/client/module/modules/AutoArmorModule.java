package net.shlomo1412.booster.client.module.modules;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds an "Auto Armor" toggle button to the inventory screen.
 * When toggled ON, continuously monitors inventory and automatically equips the best armor.
 * Works even when the inventory screen is closed - monitors via client tick events.
 * Takes into account armor value and enchantments (especially Protection).
 */
public class AutoArmorModule extends GUIModule {
    
    public static final String AUTO_ARMOR_WIDGET_ID = "auto_armor";
    
    // Persistent toggle state (saved to config)
    private final ModuleSetting.BooleanSetting autoArmorToggle;
    
    private BoosterButton autoArmorButton;
    private int equipCooldown = 0;
    private static boolean tickRegistered = false;
    
    public AutoArmorModule() {
        super(
            "auto_armor",
            "Auto Armor",
            "Toggle to automatically equip the best armor.\n" +
            "Works continuously, even when inventory is closed.\n" +
            "Considers armor value and enchantments.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Persistent toggle setting
        this.autoArmorToggle = new ModuleSetting.BooleanSetting(
            "auto_armor_active",
            "Auto Armor Active",
            "When enabled, automatically equips the best armor at all times",
            false  // Default off
        );
        registerSetting(autoArmorToggle);
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        registerTickHandler();
    }
    
    /**
     * Registers the client tick handler for continuous armor monitoring.
     * Only registers once.
     */
    public static void registerTickHandler() {
        if (!tickRegistered) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                AutoArmorModule module = ModuleManager.getInstance().getModule(AutoArmorModule.class);
                if (module != null && module.isEnabled() && module.isAutoArmorActive()) {
                    module.tickAutoArmor(client);
                }
            });
            tickRegistered = true;
        }
    }
    
    /**
     * @return Whether auto armor is currently active (toggled on)
     */
    public boolean isAutoArmorActive() {
        return autoArmorToggle.getValue();
    }
    
    /**
     * Sets the auto armor toggle state.
     */
    public void setAutoArmorActive(boolean active) {
        autoArmorToggle.setValue(active);
        ModuleManager.getInstance().saveConfig();
        updateButtonAppearance();
    }
    
    /**
     * Toggles the auto armor state.
     */
    public void toggleAutoArmor() {
        setAutoArmorActive(!isAutoArmorActive());
    }
    
    /**
     * Creates the Auto Armor button for the inventory screen.
     */
    public void createButton(HandledScreen<?> screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(AUTO_ARMOR_WIDGET_ID, 4, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        autoArmorButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            isAutoArmorActive() ? "🛡✓" : "🛡",
            "Auto Armor",
            "Toggle automatic armor equipping.\n" +
            "When ON, always wears the best armor available.\n" +
            "Works even when inventory is closed!\n\n" +
            "§7Status: " + (isAutoArmorActive() ? "§aON" : "§cOFF"),
            button -> toggleAutoArmor()
        );
        
        // Apply display mode from settings
        autoArmorButton.setDisplayMode(settings.getDisplayMode());
        
        autoArmorButton.setEditorInfo(this, AUTO_ARMOR_WIDGET_ID, "Auto Armor", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(autoArmorButton);
        
        addDrawableChild.accept(autoArmorButton);
    }
    
    /**
     * Updates button appearance based on toggle state.
     */
    private void updateButtonAppearance() {
        if (autoArmorButton != null) {
            autoArmorButton.setMessage(net.minecraft.text.Text.literal(
                isAutoArmorActive() ? "🛡✓" : "🛡"
            ));
        }
    }
    
    /**
     * Called every client tick to check and equip best armor.
     * Works even when inventory screen is not open.
     */
    private void tickAutoArmor(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.world == null) {
            return;
        }
        
        // Cooldown to prevent spam
        if (equipCooldown > 0) {
            equipCooldown--;
            return;
        }
        
        // Check and equip best armor for each slot
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            
            if (tryEquipBestArmorGlobally(client, slot)) {
                equipCooldown = 5; // Small cooldown between equips
                return; // Only equip one piece per tick
            }
        }
    }
    
    /**
     * Called from inventory screen to update the button.
     */
    public void tick(HandledScreen<?> screen) {
        // Just update button appearance
        updateButtonAppearance();
    }
    
    /**
     * Gets the equipment slot for an item stack using the EQUIPPABLE component.
     * Returns null if the item is not equippable.
     */
    private EquipmentSlot getEquipmentSlot(ItemStack stack) {
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable != null) {
            return equippable.slot();
        }
        return null;
    }
    
    /**
     * Tries to equip the best armor piece for a given slot.
     * Works by opening a synthetic container interaction.
     * @return true if an armor piece needs to be swapped
     */
    private boolean tryEquipBestArmorGlobally(MinecraftClient client, EquipmentSlot slot) {
        PlayerInventory inventory = client.player.getInventory();
        
        // Get currently equipped armor
        ItemStack currentArmor = inventory.getArmorStack(slot.getEntitySlotId());
        double currentScore = getArmorScore(currentArmor, slot);
        
        // Find best armor in inventory
        int bestSlotIndex = -1;
        double bestScore = currentScore;
        
        // Check main inventory (slots 9-35) and hotbar (0-8)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            
            // Check if this item is equippable in the target slot
            EquipmentSlot armorSlot = getEquipmentSlot(stack);
            if (armorSlot == slot) {
                double score = getArmorScore(stack, slot);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlotIndex = i;
                }
            }
        }
        
        if (bestSlotIndex == -1) {
            return false; // No better armor found
        }
        
        // We need to equip armor - use the player's screen handler
        // This works even when no screen is open because the player always has a PlayerScreenHandler
        if (client.player.currentScreenHandler instanceof PlayerScreenHandler handler) {
            // Convert inventory slot to screen handler slot
            // PlayerScreenHandler slots:
            // 0 = crafting output, 1-4 = crafting grid, 5-8 = armor slots, 9 = offhand
            // 10-36 = main inventory (actually 9-35 in inventory), 37-45 = hotbar (0-8 in inventory)
            
            int handlerSlot;
            if (bestSlotIndex < 9) {
                // Hotbar: slots 0-8 in inventory -> slots 36-44 in handler
                handlerSlot = 36 + bestSlotIndex;
            } else {
                // Main inventory: slots 9-35 in inventory -> slots 9-35 in handler
                handlerSlot = bestSlotIndex;
            }
            
            // Shift-click to equip (will swap if something is already equipped)
            client.interactionManager.clickSlot(
                handler.syncId,
                handlerSlot,
                0,
                SlotActionType.QUICK_MOVE,
                client.player
            );
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Calculates a score for an armor piece based on armor value and enchantments.
     */
    private double getArmorScore(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) {
            return 0;
        }
        
        // Check if this item is equippable in the target slot
        EquipmentSlot itemSlot = getEquipmentSlot(stack);
        if (itemSlot != slot) {
            return 0;
        }
        
        // Get armor value from attributes
        double score = 0;
        
        // Get protection value from the item's attribute modifiers
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            for (var entry : modifiers.modifiers()) {
                if (entry.attribute().equals(EntityAttributes.ARMOR)) {
                    score += entry.modifier().value();
                } else if (entry.attribute().equals(EntityAttributes.ARMOR_TOUGHNESS)) {
                    score += entry.modifier().value() * 2;
                }
            }
        }
        
        // Add enchantment bonuses
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                // Protection enchantments add significant value
                score += getEnchantmentLevel(enchantments, Enchantments.PROTECTION) * 3;
                score += getEnchantmentLevel(enchantments, Enchantments.FIRE_PROTECTION) * 2;
                score += getEnchantmentLevel(enchantments, Enchantments.BLAST_PROTECTION) * 2;
                score += getEnchantmentLevel(enchantments, Enchantments.PROJECTILE_PROTECTION) * 2;
                
                // Unbreaking adds longevity value
                score += getEnchantmentLevel(enchantments, Enchantments.UNBREAKING) * 1;
                
                // Mending is very valuable
                score += getEnchantmentLevel(enchantments, Enchantments.MENDING) * 5;
                
                // Thorns can be considered valuable
                score += getEnchantmentLevel(enchantments, Enchantments.THORNS) * 1;
                
                // Special helmet enchantments
                if (slot == EquipmentSlot.HEAD) {
                    score += getEnchantmentLevel(enchantments, Enchantments.RESPIRATION) * 2;
                    score += getEnchantmentLevel(enchantments, Enchantments.AQUA_AFFINITY) * 2;
                }
                
                // Special boots enchantments
                if (slot == EquipmentSlot.FEET) {
                    score += getEnchantmentLevel(enchantments, Enchantments.FEATHER_FALLING) * 3;
                    score += getEnchantmentLevel(enchantments, Enchantments.DEPTH_STRIDER) * 2;
                    score += getEnchantmentLevel(enchantments, Enchantments.FROST_WALKER) * 1;
                    score += getEnchantmentLevel(enchantments, Enchantments.SOUL_SPEED) * 1;
                }
            }
        }
        
        // Penalize low durability
        if (stack.isDamageable()) {
            double durabilityPercent = (double) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage();
            if (durabilityPercent < 0.1) {
                score *= 0.5; // Heavily penalize nearly broken armor
            } else if (durabilityPercent < 0.25) {
                score *= 0.8; // Moderate penalty for low durability
            }
        }
        
        return score;
    }
    
    /**
     * Gets the level of a specific enchantment on an item.
     * Iterates through the enchantments to find a match by key.
     */
    private int getEnchantmentLevel(ItemEnchantmentsComponent enchantments, 
                                     RegistryKey<Enchantment> enchantmentKey) {
        // Iterate through enchantments and check for matching key
        for (var entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(enchantmentKey)) {
                return enchantments.getLevel(entry);
            }
        }
        
        return 0;
    }
    
    /**
     * Gets the button.
     */
    public BoosterButton getButton() {
        return autoArmorButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        autoArmorButton = null;
    }
}
