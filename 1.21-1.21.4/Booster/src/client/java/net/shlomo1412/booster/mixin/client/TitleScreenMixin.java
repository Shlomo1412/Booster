package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.LastServerModule;
import net.shlomo1412.booster.client.module.modules.LastWorldModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject Booster widgets into the title screen.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Unique
    private LastServerModule booster$lastServerModule;
    
    @Unique
    private LastWorldModule booster$lastWorldModule;

    protected TitleScreenMixin(net.minecraft.text.Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void booster$onInit(CallbackInfo ci) {
        if (!ModuleManager.getInstance().isInitialized()) {
            return;
        }

        booster$lastServerModule = null;
        booster$lastWorldModule = null;
        
        TitleScreen self = (TitleScreen) (Object) this;
        
        // Calculate anchor position - below the main menu buttons
        // The buttons are centered, so we use screen center as anchor X
        int anchorX = this.width / 2 - 100;  // Same as vanilla button X
        int anchorY = this.height / 4 + 48 + 72 + 12;  // Below the main buttons with some padding
        
        // Add Last World button
        booster$lastWorldModule = ModuleManager.getInstance().getModule(LastWorldModule.class);
        if (booster$lastWorldModule != null && booster$lastWorldModule.isEnabled()) {
            booster$lastWorldModule.createButton(
                self,
                anchorX,
                anchorY,
                button -> this.addDrawableChild(button)
            );
        }
        
        // Add Last Server button (next to Last World)
        booster$lastServerModule = ModuleManager.getInstance().getModule(LastServerModule.class);
        if (booster$lastServerModule != null && booster$lastServerModule.isEnabled()) {
            booster$lastServerModule.createButton(
                self,
                anchorX + 102,  // Next to Last World button
                anchorY,
                button -> this.addDrawableChild(button)
            );
        }
    }
}
