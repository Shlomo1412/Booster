package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.shlomo1412.booster.client.module.AlertModule;
import net.shlomo1412.booster.client.module.ModuleSetting;

/**
 * Module that alerts the player when their health is low.
 * Fully customizable with threshold, message type, color, format, sound, and cooldown.
 */
public class LowHealthAlertModule extends AlertModule {
    
    private final ModuleSetting.NumberSetting thresholdSetting;
    private final ModuleSetting.BooleanSetting showExactHealthSetting;
    
    // Track to avoid spam when health stays low
    private float lastAlertedHealth = -1;
    private boolean wasAboveThreshold = true;
    
    public LowHealthAlertModule() {
        super(
            "low_health_alert",
            "Low Health Alert",
            "Alerts when your health drops below a threshold.\n" +
            "Fully customizable: threshold, display type, color, format, sound.",
            true,
            0xFFFF0000,  // Bright red
            MessageType.TITLE,
            3  // 3 second cooldown
        );
        
        // Threshold setting (in half-hearts, so 6 = 3 hearts)
        this.thresholdSetting = new ModuleSetting.NumberSetting(
            "threshold",
            "Threshold (Half-Hearts)",
            "Health threshold in half-hearts (20 = full health, 6 = 3 hearts)",
            6,
            1,
            19
        );
        registerSetting(thresholdSetting);
        
        // Show exact health setting
        this.showExactHealthSetting = new ModuleSetting.BooleanSetting(
            "show_exact_health",
            "Show Exact Health",
            "Show exact health value in the alert message",
            true
        );
        registerSetting(showExactHealthSetting);
    }
    
    @Override
    protected void checkAndAlert(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int threshold = thresholdSetting.getValue();
        
        // Check if health dropped below threshold
        if (health <= threshold && health > 0) {
            // Only alert if we just crossed the threshold or health decreased
            boolean justCrossedThreshold = wasAboveThreshold;
            boolean healthDecreased = lastAlertedHealth > 0 && health < lastAlertedHealth;
            
            if ((justCrossedThreshold || healthDecreased) && canAlert()) {
                // Build alert message
                String message;
                if (showExactHealthSetting.getValue()) {
                    float hearts = health / 2.0f;
                    float maxHearts = maxHealth / 2.0f;
                    int percent = (int) ((health / maxHealth) * 100);
                    message = String.format("❤ LOW HEALTH: %.1f/%.1f (%d%%)", hearts, maxHearts, percent);
                } else {
                    message = "❤ LOW HEALTH!";
                }
                
                sendAlert(message);
                lastAlertedHealth = health;
                wasAboveThreshold = false;
            }
        } else if (health > threshold) {
            // Reset when health recovers
            wasAboveThreshold = true;
            lastAlertedHealth = -1;
        }
    }
    
    @Override
    protected void onDisable() {
        lastAlertedHealth = -1;
        wasAboveThreshold = true;
    }
}
