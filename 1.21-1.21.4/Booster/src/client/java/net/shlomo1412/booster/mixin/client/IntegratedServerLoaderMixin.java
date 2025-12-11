package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.module.modules.LastWorldModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Mixin to track when a player loads a singleplayer world.
 * Records the world for the "Last World" feature.
 */
@Mixin(IntegratedServerLoader.class)
public abstract class IntegratedServerLoaderMixin {
    
    @Inject(method = "start(Ljava/lang/String;Ljava/lang/Runnable;)V", at = @At("HEAD"))
    private void booster$onStart(String worldName, Runnable onCancel, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        try {
            // Get the world's display name from level storage
            LevelStorage levelStorage = client.getLevelStorage();
            if (levelStorage.levelExists(worldName)) {
                // Start with folder name as fallback
                String displayName = worldName;
                
                try {
                    // Load summaries to find the display name
                    LevelStorage.LevelList levelList = levelStorage.getLevelList();
                    CompletableFuture<List<LevelSummary>> future = levelStorage.loadSummaries(levelList);
                    List<LevelSummary> summaries = future.join();
                    
                    for (LevelSummary summary : summaries) {
                        if (summary.getName().equals(worldName)) {
                            displayName = summary.getDisplayName();
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Fall back to folder name
                    BoosterClient.LOGGER.debug("Could not get display name for world: " + worldName, e);
                }
                
                BoosterClient.LOGGER.info("Recording last world: {} ({})", displayName, worldName);
                LastWorldModule.setLastWorld(worldName, displayName);
            }
        } catch (Exception e) {
            BoosterClient.LOGGER.error("Failed to record last world: " + worldName, e);
        }
    }
}
