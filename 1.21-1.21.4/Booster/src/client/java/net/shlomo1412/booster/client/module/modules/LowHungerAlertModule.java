package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.HungerManager;
import net.shlomo1412.booster.client.module.AlertModule;
import net.shlomo1412.booster.client.module.ModuleSetting;

/**
 * Module that alerts the player when their hunger is low.
 * Fully customizable with threshold, message type, color, format, sound, and cooldown.
 */
public class LowHungerAlertModule extends AlertModule {
    
    private final ModuleSetting.NumberSetting thresholdSetting;
    private final ModuleSetting.BooleanSetting showExactHungerSetting;
    private final ModuleSetting.BooleanSetting alertOnSaturationSetting;
    
    // Track to avoid spam when hunger stays low
    private int lastAlertedHunger = -1;
    private boolean wasAboveThreshold = true;
    
    public LowHungerAlertModule() {
        super(
            "low_hunger_alert",
            "Low Hunger Alert",
            "Alerts when your hunger drops below a threshold.\n" +
            "Fully customizable: threshold, display type, color, format, sound.",
            true,
            0xFFFFAA00,  // Orange color
            MessageType.ACTION_BAR,
            5  // 5 second cooldown
        );
        
        // Threshold setting (in half-drumsticks, so 6 = 3 drumsticks)
        this.thresholdSetting = new ModuleSetting.NumberSetting(
            "threshold",
            "Threshold (Half-Drumsticks)",
            "Hunger threshold in half-drumsticks (20 = full hunger, 6 = 3 drumsticks)",
            6,
            1,
            19
        );
        registerSetting(thresholdSetting);
        
        // Show exact hunger setting
        this.showExactHungerSetting = new ModuleSetting.BooleanSetting(
            "show_exact_hunger",
            "Show Exact Value",
            "Show exact hunger value in the alert message",
            true
        );
        registerSetting(showExactHungerSetting);
        
        // Alert when saturation is low (means hunger will drop soon)
        this.alertOnSaturationSetting = new ModuleSetting.BooleanSetting(
            "alert_saturation",
            "Alert Low Saturation",
            "Also alert when saturation is low (hunger will drop soon)",
            false
        );
        registerSetting(alertOnSaturationSetting);
    }
    
    @Override
    protected void checkAndAlert(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        HungerManager hunger = player.getHungerManager();
        int foodLevel = hunger.getFoodLevel();
        float saturation = hunger.getSaturationLevel();
        int threshold = thresholdSetting.getValue();
        
        // Check hunger level
        if (foodLevel <= threshold && foodLevel > 0) {
            boolean justCrossedThreshold = wasAboveThreshold;
            boolean hungerDecreased = lastAlertedHunger > 0 && foodLevel < lastAlertedHunger;
            
            if ((justCrossedThreshold || hungerDecreased) && canAlert()) {
                // Use title + subtitle for better display
                float drumsticks = foodLevel / 2.0f;
                int percent = (int) ((foodLevel / 20.0) * 100);
                
                String title = "🍖 LOW HUNGER!";
                String subtitle = String.format("%.1f/10 drumsticks (%d%%)", drumsticks, percent);
                
                if (showExactHungerSetting.getValue()) {
                    sendAlert(title, subtitle);
                } else {
                    sendAlert(title);
                }
                
                lastAlertedHunger = foodLevel;
                wasAboveThreshold = false;
            }
        } else if (foodLevel > threshold) {
            // Reset when hunger recovers
            wasAboveThreshold = true;
            lastAlertedHunger = -1;
            
            // Check saturation if enabled and hunger is full but saturation is low
            if (alertOnSaturationSetting.getValue() && saturation < 1.0f && foodLevel > 17) {
                if (canAlert()) {
                    sendAlert("🍖 LOW SATURATION!", "Eat soon to restore stamina");
                }
            }
        }
    }
    
    @Override
    protected void onDisable() {
        lastAlertedHunger = -1;
        wasAboveThreshold = true;
    }
}
