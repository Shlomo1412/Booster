package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.shlomo1412.booster.client.module.AlertModule;
import net.shlomo1412.booster.client.module.ModuleSetting;

/**
 * Module that alerts the player when their air (oxygen) is low while underwater.
 * Fully customizable with threshold, message type, color, format, sound, and cooldown.
 */
public class LowAirAlertModule extends AlertModule {
    
    private final ModuleSetting.NumberSetting thresholdSetting;
    private final ModuleSetting.BooleanSetting showExactAirSetting;
    
    // Track to avoid spam
    private int lastAlertedAir = -1;
    private boolean wasAboveThreshold = true;
    
    // Max air in vanilla Minecraft
    private static final int MAX_AIR = 300; // 15 seconds (20 ticks/second * 15 = 300)
    
    public LowAirAlertModule() {
        super(
            "low_air_alert",
            "Low Air Alert",
            "Alerts when your air (oxygen) is low while underwater.\n" +
            "Fully customizable: threshold, display type, color, format, sound.",
            true,
            0xFF00AAFF,  // Cyan/blue color
            MessageType.TITLE,
            2  // 2 second cooldown
        );
        
        // Threshold setting (in percent of max air)
        this.thresholdSetting = new ModuleSetting.NumberSetting(
            "threshold",
            "Threshold (%)",
            "Air threshold as percentage (50% = 7.5 seconds of air left)",
            30,
            5,
            90
        );
        registerSetting(thresholdSetting);
        
        // Show exact air setting
        this.showExactAirSetting = new ModuleSetting.BooleanSetting(
            "show_exact_air",
            "Show Seconds Left",
            "Show exact seconds of air remaining",
            true
        );
        registerSetting(showExactAirSetting);
    }
    
    @Override
    protected void checkAndAlert(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        int air = player.getAir();
        int maxAir = player.getMaxAir();
        
        // Only check when underwater (air is below max)
        if (air >= maxAir) {
            // Not underwater, reset tracking
            wasAboveThreshold = true;
            lastAlertedAir = -1;
            return;
        }
        
        // Calculate threshold in air ticks
        int thresholdTicks = (int) (maxAir * (thresholdSetting.getValue() / 100.0));
        
        if (air <= thresholdTicks && air > 0) {
            boolean justCrossedThreshold = wasAboveThreshold;
            boolean airDecreased = lastAlertedAir > 0 && air < lastAlertedAir;
            
            if ((justCrossedThreshold || airDecreased) && canAlert()) {
                // Use title + subtitle for better display
                float secondsLeft = air / 20.0f;  // 20 ticks per second
                int percent = (int) ((air / (double) maxAir) * 100);
                
                String title = "🫧 LOW OXYGEN!";
                String subtitle = String.format("%.1f seconds remaining!", secondsLeft);
                
                if (showExactAirSetting.getValue()) {
                    sendAlert(title, subtitle);
                } else {
                    sendAlert(title);
                }
                
                lastAlertedAir = air;
                wasAboveThreshold = false;
            }
        } else if (air > thresholdTicks) {
            // Reset when air recovers (surfaced briefly)
            wasAboveThreshold = true;
            lastAlertedAir = -1;
        } else if (air <= 0) {
            // Out of air - drowning!
            if (canAlert()) {
                sendAlert("🫧 DROWNING!", "Get to the surface!");
            }
        }
    }
    
    @Override
    protected void onDisable() {
        lastAlertedAir = -1;
        wasAboveThreshold = true;
    }
}
