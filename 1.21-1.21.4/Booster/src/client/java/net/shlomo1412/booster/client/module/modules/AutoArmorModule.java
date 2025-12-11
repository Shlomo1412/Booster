package net.shlomo1412.booster.client.module.modules;

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
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds an "Auto Armor" toggle button to the inventory screen.
 * When enabled, automatically equips the best armor found in the inventory.
 * Takes into account armor value and enchantments (especially Protection).
 */
public class AutoArmorModule extends GUIModule {
    
    public static final String AUTO_ARMOR_WIDGET_ID = "auto_armor";
    
    private BoosterButton autoArmorButton;
    private boolean isAutoEquipping = false;
    private int equipDelay = 0;
    
    public AutoArmorModule() {
        super(
            "auto_armor",
            "Auto Armor",
            "Adds a button to automatically equip the best armor.\n" +
            "Considers armor value and enchantments.",
            true,
            20,  // Default button width
            20   // Default button height
        );
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
            "🛡",
            "Auto Armor",
            "Click to automatically equip the best armor from your inventory.\n" +
            "Compares armor value and enchantments to find the best pieces.",
            button -> startAutoEquip(screen)
        );
        
        // Apply display mode from settings
        autoArmorButton.setDisplayMode(settings.getDisplayMode());
        
        autoArmorButton.setEditorInfo(this, AUTO_ARMOR_WIDGET_ID, "Auto Armor", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(autoArmorButton);
        
        addDrawableChild.accept(autoArmorButton);
    }
    
    /**
     * Starts the auto-equip process.
     */
    private void startAutoEquip(HandledScreen<?> screen) {
        if (isAutoEquipping) {
            // Already running, stop it
            stopAutoEquip();
            return;
        }
        
        isAutoEquipping = true;
        equipDelay = 0;
        updateButtonAppearance();
    }
    
    /**
     * Updates button appearance based on state.
     */
    private void updateButtonAppearance() {
        if (autoArmorButton != null) {
            autoArmorButton.setMessage(net.minecraft.text.Text.literal(
                isAutoEquipping ? "⏳" : "🛡"
            ));
        }
    }
    
    /**
     * Called every tick to perform armor equipping.
     */
    public void tick(HandledScreen<?> screen) {
        if (!isAutoEquipping || !isEnabled()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) {
            stopAutoEquip();
            return;
        }
        
        if (!(screen.getScreenHandler() instanceof PlayerScreenHandler handler)) {
            stopAutoEquip();
            return;
        }
        
        // Small delay between operations
        if (equipDelay > 0) {
            equipDelay--;
            return;
        }
        
        // Try to equip best armor for each slot
        boolean equipped = false;
        
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            
            if (tryEquipBestArmor(client, handler, slot)) {
                equipped = true;
                equipDelay = 2; // Small delay between equips
                break;
            }
        }
        
        if (!equipped) {
            // Done equipping
            stopAutoEquip();
        }
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
     * @return true if an armor piece was equipped
     */
    private boolean tryEquipBestArmor(MinecraftClient client, PlayerScreenHandler handler, EquipmentSlot slot) {
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
        
        // Convert inventory slot to screen handler slot
        // PlayerScreenHandler slots:
        // 0 = crafting output, 1-4 = crafting grid, 5-8 = armor slots, 9 = offhand
        // 10-36 = main inventory, 37-45 = hotbar
        
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return 0;
        }
        
        // Iterate through enchantments and check for matching key
        for (var entry : enchantments.getEnchantments()) {
            if (entry.matchesKey(enchantmentKey)) {
                return enchantments.getLevel(entry);
            }
        }
        
        return 0;
    }
    
    /**
     * Stops the auto-equip process.
     */
    public void stopAutoEquip() {
        isAutoEquipping = false;
        equipDelay = 0;
        updateButtonAppearance();
    }
    
    /**
     * @return Whether auto-equipping is currently active
     */
    public boolean isAutoEquipping() {
        return isAutoEquipping;
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
        stopAutoEquip();
        autoArmorButton = null;
    }
}
