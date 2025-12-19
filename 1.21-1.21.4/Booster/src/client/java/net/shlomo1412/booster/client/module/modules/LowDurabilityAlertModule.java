package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.shlomo1412.booster.client.module.AlertModule;
import net.shlomo1412.booster.client.module.ModuleSetting;

/**
 * Module that alerts the player when their held item's durability is low.
 * Fully customizable with threshold, message type, color, format, sound, and cooldown.
 */
public class LowDurabilityAlertModule extends AlertModule {
    
    private final ModuleSetting.NumberSetting thresholdSetting;
    private final ModuleSetting.BooleanSetting percentModeSetting;
    private final ModuleSetting.BooleanSetting checkOffhandSetting;
    
    // Track last warned items to avoid spam
    private ItemStack lastWarnedMainHand = ItemStack.EMPTY;
    private ItemStack lastWarnedOffHand = ItemStack.EMPTY;
    private int lastWarnedMainHandDurability = -1;
    private int lastWarnedOffHandDurability = -1;
    
    public LowDurabilityAlertModule() {
        super(
            "low_durability_alert",
            "Low Durability Alert",
            "Alerts when your held item's durability is low.\n" +
            "Fully customizable: threshold, display type, color, format, sound.",
            true,
            0xFFFF5555,  // Red color
            MessageType.ACTION_BAR,
            5  // 5 second cooldown
        );
        
        // Threshold setting
        this.thresholdSetting = new ModuleSetting.NumberSetting(
            "threshold",
            "Threshold",
            "Durability threshold to trigger alert (in % or absolute)",
            10,
            1,
            100
        );
        registerSetting(thresholdSetting);
        
        // Percent mode setting
        this.percentModeSetting = new ModuleSetting.BooleanSetting(
            "percent_mode",
            "Use Percentage",
            "When enabled, threshold is % of max durability. When disabled, threshold is absolute durability points.",
            true
        );
        registerSetting(percentModeSetting);
        
        // Check offhand setting
        this.checkOffhandSetting = new ModuleSetting.BooleanSetting(
            "check_offhand",
            "Check Offhand",
            "Also check the item in your offhand",
            true
        );
        registerSetting(checkOffhandSetting);
    }
    
    @Override
    protected void checkAndAlert(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        // Check main hand
        ItemStack mainHand = player.getMainHandStack();
        checkItemDurability(mainHand, "Main Hand", true);
        
        // Check offhand if enabled
        if (checkOffhandSetting.getValue()) {
            ItemStack offHand = player.getOffHandStack();
            checkItemDurability(offHand, "Offhand", false);
        }
    }
    
    private void checkItemDurability(ItemStack stack, String slotName, boolean isMainHand) {
        if (stack.isEmpty()) {
            // Reset tracking when slot is empty
            if (isMainHand) {
                lastWarnedMainHand = ItemStack.EMPTY;
                lastWarnedMainHandDurability = -1;
            } else {
                lastWarnedOffHand = ItemStack.EMPTY;
                lastWarnedOffHandDurability = -1;
            }
            return;
        }
        
        // Check if item has durability
        Integer maxDamage = stack.get(DataComponentTypes.MAX_DAMAGE);
        if (maxDamage == null || maxDamage <= 0) return;
        
        Integer damage = stack.get(DataComponentTypes.DAMAGE);
        if (damage == null) damage = 0;
        
        int currentDurability = maxDamage - damage;
        
        // Calculate threshold
        int threshold;
        if (percentModeSetting.getValue()) {
            threshold = (int) (maxDamage * (thresholdSetting.getValue() / 100.0));
        } else {
            threshold = thresholdSetting.getValue();
        }
        
        // Check if below threshold
        if (currentDurability <= threshold && currentDurability > 0) {
            // Check if this is a new warning (different item or durability decreased)
            ItemStack lastWarned = isMainHand ? lastWarnedMainHand : lastWarnedOffHand;
            int lastDurability = isMainHand ? lastWarnedMainHandDurability : lastWarnedOffHandDurability;
            
            boolean isNewWarning = !ItemStack.areItemsEqual(stack, lastWarned) || 
                                   currentDurability < lastDurability;
            
            if (isNewWarning && canAlert()) {
                // Update tracking
                if (isMainHand) {
                    lastWarnedMainHand = stack.copy();
                    lastWarnedMainHandDurability = currentDurability;
                } else {
                    lastWarnedOffHand = stack.copy();
                    lastWarnedOffHandDurability = currentDurability;
                }
                
                // Send alert - use title + subtitle for better display
                String itemName = stack.getName().getString();
                int percent = (int) ((currentDurability / (double) maxDamage) * 100);
                
                String title = "⚠ LOW DURABILITY!";
                String subtitle = String.format("%s: %d/%d (%d%%)", itemName, currentDurability, maxDamage, percent);
                
                sendAlert(title, subtitle);
            }
        } else if (currentDurability > threshold) {
            // Reset warning if durability recovered
            if (isMainHand) {
                lastWarnedMainHand = ItemStack.EMPTY;
                lastWarnedMainHandDurability = -1;
            } else {
                lastWarnedOffHand = ItemStack.EMPTY;
                lastWarnedOffHandDurability = -1;
            }
        }
    }
    
    @Override
    protected void onDisable() {
        lastWarnedMainHand = ItemStack.EMPTY;
        lastWarnedOffHand = ItemStack.EMPTY;
        lastWarnedMainHandDurability = -1;
        lastWarnedOffHandDurability = -1;
    }
}
