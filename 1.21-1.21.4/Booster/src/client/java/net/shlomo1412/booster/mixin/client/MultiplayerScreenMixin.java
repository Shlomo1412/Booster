package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.CopyIPModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject Booster widgets into the multiplayer/server selection screen.
 */
@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {

    @Shadow
    protected MultiplayerServerListWidget serverListWidget;

    @Unique
    private CopyIPModule booster$copyIPModule;

    protected MultiplayerScreenMixin(net.minecraft.text.Text title) {
        super(title);
    }
    
    @Unique
    private ServerInfo booster$getSelectedServer() {
        if (serverListWidget == null) {
            return null;
        }
        MultiplayerServerListWidget.Entry entry = serverListWidget.getSelectedOrNull();
        if (entry instanceof MultiplayerServerListWidget.ServerEntry serverEntry) {
            return serverEntry.getServer();
        }
        return null;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void booster$onInit(CallbackInfo ci) {
        if (!ModuleManager.getInstance().isInitialized()) {
            return;
        }

        booster$copyIPModule = null;
        
        MultiplayerScreen self = (MultiplayerScreen) (Object) this;
        
        // Anchor near the bottom buttons
        int anchorX = this.width / 2 + 4 + 76 + 4;  // After "Join Server" button
        int anchorY = this.height - 52;  // Same row as buttons
        
        // Add Copy IP button
        booster$copyIPModule = ModuleManager.getInstance().getModule(CopyIPModule.class);
        if (booster$copyIPModule != null && booster$copyIPModule.isEnabled()) {
            booster$copyIPModule.createButton(
                self,
                anchorX,
                anchorY,
                this::booster$getSelectedServer,
                button -> this.addDrawableChild(button)
            );
        }
    }
}
