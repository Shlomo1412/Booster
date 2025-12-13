package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds a "Teleport to Death" button on the death screen.
 * Respawns and teleports the player to their death location.
 * Only shown when the player has operator permissions.
 */
public class TeleportToDeathModule extends GUIModule {
    
    public static final String TP_DEATH_WIDGET_ID = "teleport_to_death";
    
    private final ModuleSetting.BooleanSetting requireOpSetting;
    
    private BoosterButton tpButton;
    
    // Pending teleport after respawn
    private static boolean pendingTeleport = false;
    private static BlockPos pendingTeleportPos = null;
    
    public TeleportToDeathModule() {
        super(
            "teleport_to_death",
            "Teleport to Death",
            "Adds a button to respawn and teleport to your death location.\n" +
            "Requires operator permissions by default.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Require OP setting
        this.requireOpSetting = new ModuleSetting.BooleanSetting(
            "require_op",
            "Require OP",
            "Only show button when player has operator permissions",
            true
        );
        registerSetting(requireOpSetting);
    }
    
    /**
     * Checks if the player has operator permissions.
     */
    public static boolean hasOperatorPermission() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        // Check if player has permission level >= 2 (can use /tp)
        return client.player.hasPermissionLevel(2);
    }
    
    /**
     * Returns whether this module should be shown.
     */
    public boolean shouldShow() {
        if (!requireOpSetting.getValue()) {
            return true; // Show regardless of OP
        }
        return hasOperatorPermission();
    }
    
    /**
     * Creates the teleport button for the death screen.
     */
    public void createButton(DeathScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        // Check if we should show based on OP requirement
        if (!shouldShow()) {
            return;
        }
        
        // Check if we have death coordinates
        if (!DeathCoordinatesModule.hasDeathLocation()) {
            return;
        }
        
        WidgetSettings settings = getWidgetSettings(TP_DEATH_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        tpButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🎯",
            "TP to Death",
            "Respawn and teleport to your death location.\n" +
            "Uses /tp command (requires operator permissions).",
            button -> teleportToDeath()
        );
        
        // Apply display mode
        tpButton.setDisplayMode(settings.getDisplayMode());
        
        tpButton.setEditorInfo(this, TP_DEATH_WIDGET_ID, "TP to Death", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(tpButton);
        
        addDrawableChild.accept(tpButton);
    }
    
    /**
     * Initiates teleport to death location.
     * First respawns, then teleports after respawn.
     */
    private void teleportToDeath() {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockPos deathPos = DeathCoordinatesModule.getLastDeathPos();
        
        if (client.player == null || deathPos == null) {
            return;
        }
        
        // Store pending teleport
        pendingTeleport = true;
        pendingTeleportPos = deathPos;
        
        // Trigger respawn
        client.player.requestRespawn();
        
        BoosterClient.LOGGER.info("Respawning and teleporting to death location: {}", deathPos);
    }
    
    /**
     * Called after player respawns to execute pending teleport.
     */
    public static void onPlayerRespawn() {
        if (!pendingTeleport || pendingTeleportPos == null) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            pendingTeleport = false;
            pendingTeleportPos = null;
            return;
        }
        
        // Execute teleport command
        String command = String.format("tp %d %d %d", 
            pendingTeleportPos.getX(), 
            pendingTeleportPos.getY(), 
            pendingTeleportPos.getZ());
        
        // Send command
        client.player.networkHandler.sendChatCommand(command);
        
        BoosterClient.LOGGER.info("Executed teleport to death location: {}", pendingTeleportPos);
        
        // Clear pending state
        pendingTeleport = false;
        pendingTeleportPos = null;
    }
    
    /**
     * Checks if there's a pending teleport.
     */
    public static boolean hasPendingTeleport() {
        return pendingTeleport;
    }
    
    /**
     * Gets the button.
     */
    public BoosterButton getButton() {
        return tpButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        tpButton = null;
    }
}
