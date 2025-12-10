package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.SaveQuitGameModule;
import net.shlomo1412.booster.client.module.modules.SaveQuitToServersModule;
import net.shlomo1412.booster.client.module.modules.SaveQuitToWorldsModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject Booster widgets into the game menu (pause menu).
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    @Unique
    private SaveQuitGameModule booster$saveQuitGameModule;
    
    @Unique
    private SaveQuitToWorldsModule booster$saveQuitToWorldsModule;
    
    @Unique
    private SaveQuitToServersModule booster$saveQuitToServersModule;

    protected GameMenuScreenMixin(net.minecraft.text.Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void booster$onInit(CallbackInfo ci) {
        if (!ModuleManager.getInstance().isInitialized()) {
            return;
        }

        booster$saveQuitGameModule = null;
        booster$saveQuitToWorldsModule = null;
        booster$saveQuitToServersModule = null;
        
        GameMenuScreen self = (GameMenuScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        boolean isSingleplayer = client.isInSingleplayer();
        
        // Anchor position - below the main menu buttons
        // The pause menu buttons are centered
        int anchorX = this.width / 2 - 102;
        int anchorY = this.height / 4 + 120 + 12;  // Below the standard buttons
        
        // Add Save & Quit Game button (works for both SP and MP)
        booster$saveQuitGameModule = ModuleManager.getInstance().getModule(SaveQuitGameModule.class);
        if (booster$saveQuitGameModule != null && booster$saveQuitGameModule.isEnabled()) {
            booster$saveQuitGameModule.createButton(
                self,
                anchorX,
                anchorY,
                button -> this.addDrawableChild(button)
            );
        }
        
        // Add Save & Quit to Worlds button (singleplayer only)
        if (isSingleplayer) {
            booster$saveQuitToWorldsModule = ModuleManager.getInstance().getModule(SaveQuitToWorldsModule.class);
            if (booster$saveQuitToWorldsModule != null && booster$saveQuitToWorldsModule.isEnabled()) {
                booster$saveQuitToWorldsModule.createButton(
                    self,
                    anchorX + 122,  // Next to Save & Quit Game
                    anchorY,
                    button -> this.addDrawableChild(button)
                );
            }
        }
        
        // Add Quit to Servers button (multiplayer only)
        if (!isSingleplayer) {
            booster$saveQuitToServersModule = ModuleManager.getInstance().getModule(SaveQuitToServersModule.class);
            if (booster$saveQuitToServersModule != null && booster$saveQuitToServersModule.isEnabled()) {
                booster$saveQuitToServersModule.createButton(
                    self,
                    anchorX + 122,  // Next to Save & Quit Game
                    anchorY,
                    button -> this.addDrawableChild(button)
                );
            }
        }
    }
}
