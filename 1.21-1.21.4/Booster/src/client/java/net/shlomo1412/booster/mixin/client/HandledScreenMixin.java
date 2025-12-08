package net.shlomo1412.booster.mixin.client;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.StealStoreModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject Booster widgets into container screens.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {

    @Shadow
    @Final
    protected T handler;

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Shadow
    protected int backgroundWidth;

    @Shadow
    protected int backgroundHeight;

    // Required for extending Screen
    protected HandledScreenMixin() {
        super(null);
    }

    /**
     * Injects after the screen is initialized to add our buttons.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void booster$onInit(CallbackInfo ci) {
        if (!ModuleManager.getInstance().isInitialized()) {
            return;
        }

        // Add Steal/Store buttons
        StealStoreModule stealStoreModule = ModuleManager.getInstance().getModule(StealStoreModule.class);
        if (stealStoreModule != null && stealStoreModule.isEnabled()) {
            HandledScreen<?> self = (HandledScreen<?>) (Object) this;
            stealStoreModule.createButtons(
                self,
                handler,
                x,
                y,
                backgroundWidth,
                button -> this.addDrawableChild(button)
            );
        }
    }
}
