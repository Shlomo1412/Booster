package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.module.modules.LastServerModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to track when a player joins a multiplayer server.
 * Records the server for the "Last Server" feature.
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
}
