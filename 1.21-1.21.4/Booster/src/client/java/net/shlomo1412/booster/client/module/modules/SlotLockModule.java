package net.shlomo1412.booster.client.module.modules;

import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;

/**
 * Config holder for slot lock visuals.
 */
public class SlotLockModule extends GUIModule {
    private final ModuleSetting.ColorSetting softLockColor;
    private final ModuleSetting.ColorSetting hardLockColor;

    public SlotLockModule() {
        super(
            "slot_lock",
            "Slot Lock",
            "ALT+Click a slot to soft-lock it (ignored by sorting/moving).\n" +
            "SHIFT+ALT+Click makes a hard lock (cannot be picked up manually).",
            true,
            20,
            20
        );

        softLockColor = new ModuleSetting.ColorSetting(
            "soft_lock_color",
            "Soft Lock Color",
            "Overlay color for soft-locked slots",
            0x6633AAFF
        );
        hardLockColor = new ModuleSetting.ColorSetting(
            "hard_lock_color",
            "Hard Lock Color",
            "Overlay color for hard-locked slots",
            0x66FF7A33
        );

        registerSetting(softLockColor);
        registerSetting(hardLockColor);
    }

    public int getSoftLockColor() {
        return softLockColor.getValue();
    }

    public int getHardLockColor() {
        return hardLockColor.getValue();
    }
}
