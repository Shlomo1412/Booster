package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.module.modules.DeathCoordinatesModule;
import net.shlomo1412.booster.client.module.modules.DeathInventoryModule;
import net.shlomo1412.booster.client.module.modules.LastServerModule;
import net.shlomo1412.booster.client.module.modules.RecoverItemsModule;
import net.shlomo1412.booster.client.module.modules.TeleportToDeathModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to track player events:
 * - Joining a multiplayer server (for "Last Server" feature)
 * - Player death (for death screen modules)
 * - Player respawn (for teleport to death feature)
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    
    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void booster$onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Only track multiplayer servers, not singleplayer
        if (!client.isInSingleplayer()) {
            ServerInfo serverInfo = client.getCurrentServerEntry();
            if (serverInfo != null) {
                String serverName = serverInfo.name;
                String serverAddress = serverInfo.address;
                
                BoosterClient.LOGGER.info("Recording last server: {} ({})", serverName, serverAddress);
                LastServerModule.setLastServer(serverName, serverAddress);
            }
        }
    }
    
    @Inject(method = "onDeathMessage", at = @At("HEAD"))
    private void booster$onDeathMessage(DeathMessageS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        
        if (player == null) return;
        
        // Record death position
        BlockPos deathPos = player.getBlockPos();
        String dimension = player.getWorld().getRegistryKey().getValue().toString();
        
        BoosterClient.LOGGER.info("Recording death location: {} in {}", deathPos, dimension);
        DeathCoordinatesModule.recordDeathLocation(deathPos, dimension);
        
        // Record death inventory
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> mainInventory = new ArrayList<>();
        for (int i = 0; i < inventory.main.size(); i++) {
            mainInventory.add(inventory.main.get(i));
        }
        
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < inventory.armor.size(); i++) {
            armor[i] = inventory.armor.get(i);
        }
        
        ItemStack offhand = inventory.offHand.isEmpty() ? ItemStack.EMPTY : inventory.offHand.getFirst();
        
        DeathInventoryModule.recordDeathInventory(mainInventory, armor, offhand);
        BoosterClient.LOGGER.info("Recorded death inventory with {} items", mainInventory.size());
    }
    
    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    private void booster$onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        // Handle pending teleport to death location
        if (TeleportToDeathModule.hasPendingTeleport()) {
            // Delay the teleport slightly to ensure player is fully spawned
            MinecraftClient.getInstance().execute(() -> {
                TeleportToDeathModule.onPlayerRespawn();
            });
        }
        
        // Handle pending item recovery
        if (RecoverItemsModule.hasPendingRecovery()) {
            MinecraftClient.getInstance().execute(() -> {
                RecoverItemsModule.onPlayerRespawn();
            });
        }
    }
}
