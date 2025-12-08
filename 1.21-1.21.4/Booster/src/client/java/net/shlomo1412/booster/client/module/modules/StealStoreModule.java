package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that adds Steal and Store buttons to container screens.
 * - Steal: Takes all items from the container to the player's inventory
 * - Store: Puts all items from the player's inventory into the container
 */
public class StealStoreModule extends GUIModule {
    public static final String ID = "steal_store";
    
    // Unicode icons for buttons
    public static final String STEAL_ICON = "⬇"; // Down arrow - take from container
    public static final String STORE_ICON = "⬆"; // Up arrow - put into container

    // Button dimensions
    public static final int BUTTON_WIDTH = 16;
    public static final int BUTTON_HEIGHT = 16;
    public static final int BUTTON_SPACING = 2;

    public StealStoreModule() {
        super(
            ID,
            "Steal & Store",
            "Adds buttons to container screens for quickly moving all items between your inventory and the container.",
            true, // Enabled by default
            4,    // Default X offset from container's right edge
            4     // Default Y offset from container's top
        );
    }

    /**
     * Creates the Steal button.
     *
     * @param x       X position
     * @param y       Y position
     * @param handler The screen handler for the container
     * @return The configured button widget
     */
    public ButtonWidget createStealButton(int x, int y, ScreenHandler handler) {
        return BoosterButton.builder()
            .position(x, y)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .icon(STEAL_ICON)
            .actionDescription("Steal All")
            .fullDescription("Quickly transfers all items from the container into your inventory. " +
                    "Items will be placed in the first available slots. " +
                    "If your inventory is full, remaining items will stay in the container.")
            .onPress(button -> performSteal(handler))
            .build();
    }

    /**
     * Creates the Store button.
     *
     * @param x       X position
     * @param y       Y position
     * @param handler The screen handler for the container
     * @return The configured button widget
     */
    public ButtonWidget createStoreButton(int x, int y, ScreenHandler handler) {
        return BoosterButton.builder()
            .position(x, y)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .icon(STORE_ICON)
            .actionDescription("Store All")
            .fullDescription("Quickly transfers all items from your inventory into the container. " +
                    "Items will be placed in the first available slots. " +
                    "If the container is full, remaining items will stay in your inventory.")
            .onPress(button -> performStore(handler))
            .build();
    }

    /**
     * Creates both Steal and Store buttons at the calculated positions.
     *
     * @param screen     The container screen
     * @param handler    The screen handler
     * @param containerX The X position of the container GUI
     * @param containerY The Y position of the container GUI
     * @param containerWidth The width of the container GUI
     * @param addButton  Consumer to add buttons to the screen
     */
    public void createButtons(HandledScreen<?> screen, ScreenHandler handler,
                              int containerX, int containerY, int containerWidth,
                              Consumer<ButtonWidget> addButton) {
        if (!isEnabled()) {
            return;
        }

        // Position buttons at the right side of the container
        int baseX = containerX + containerWidth + getOffsetX();
        int baseY = containerY + getOffsetY();

        // Steal button (top)
        ButtonWidget stealButton = createStealButton(baseX, baseY, handler);
        addButton.accept(stealButton);

        // Store button (below steal)
        ButtonWidget storeButton = createStoreButton(baseX, baseY + BUTTON_HEIGHT + BUTTON_SPACING, handler);
        addButton.accept(storeButton);
    }

    /**
     * Performs the steal action - moves items from container to player inventory.
     */
    private void performSteal(ScreenHandler handler) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) {
            return;
        }

        // Get all container slots (non-player inventory slots)
        for (Slot slot : handler.slots) {
            // Skip player inventory slots
            if (slot.inventory instanceof PlayerInventory) {
                continue;
            }

            // Skip empty slots
            if (!slot.hasStack()) {
                continue;
            }

            // Quick move the item (shift-click)
            client.interactionManager.clickSlot(
                handler.syncId,
                slot.id,
                0,
                SlotActionType.QUICK_MOVE,
                client.player
            );
        }
    }

    /**
     * Performs the store action - moves items from player inventory to container.
     */
    private void performStore(ScreenHandler handler) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) {
            return;
        }

        // Get all player inventory slots
        for (Slot slot : handler.slots) {
            // Only process player inventory slots (not hotbar armor, etc. in some screens)
            if (!(slot.inventory instanceof PlayerInventory)) {
                continue;
            }

            // Skip empty slots
            if (!slot.hasStack()) {
                continue;
            }

            // Quick move the item (shift-click)
            client.interactionManager.clickSlot(
                handler.syncId,
                slot.id,
                0,
                SlotActionType.QUICK_MOVE,
                client.player
            );
        }
    }
}
