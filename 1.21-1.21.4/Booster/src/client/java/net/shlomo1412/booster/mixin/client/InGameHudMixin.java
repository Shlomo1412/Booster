package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.PinEstimatedTimeModule;
import net.shlomo1412.booster.client.module.modules.ShowInventoryModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for rendering HUD overlays.
 * Handles pinned furnace time display and show inventory overlay.
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    
    /**
     * Renders Booster HUD overlays after the main HUD.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void booster$onRenderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // Render pinned furnace time overlays
        PinEstimatedTimeModule pinModule = ModuleManager.getInstance().getModule(PinEstimatedTimeModule.class);
        if (pinModule != null && pinModule.isEnabled()) {
            pinModule.renderOverlays(context);
        }
        
        // Render show inventory overlay
        ShowInventoryModule showInventoryModule = ModuleManager.getInstance().getModule(ShowInventoryModule.class);
        if (showInventoryModule != null && showInventoryModule.isEnabled()) {
            showInventoryModule.renderInventoryOverlay(context);
        }
    }
}
