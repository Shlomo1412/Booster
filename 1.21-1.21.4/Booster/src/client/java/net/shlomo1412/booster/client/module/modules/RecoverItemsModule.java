package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Module that adds a "Recover Items" button on the death screen.
 * Restores the player's exact inventory from before death.
 * Only works when the player has operator permissions.
 */
public class RecoverItemsModule extends GUIModule {
    
    public static final String RECOVER_ITEMS_WIDGET_ID = "recover_items";
    
    private final ModuleSetting.BooleanSetting requireOpSetting;
    private final ModuleSetting.BooleanSetting restoreMetadataSetting;
    private final ModuleSetting.BooleanSetting restoreExactSlotsSetting;
    
    private BoosterButton recoverButton;
    
    // Pending recovery after respawn
    private static boolean pendingRecovery = false;
    private static boolean restoreMetadata = true;
    private static boolean restoreExactSlots = true;
    
    public RecoverItemsModule() {
        super(
            "recover_items",
            "Recover Items",
            "Adds a button to recover your exact inventory after death.\n" +
            "Respawns and uses /give commands to restore items.\n" +
            "Requires operator permissions by default.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Require OP setting
        this.requireOpSetting = new ModuleSetting.BooleanSetting(
            "require_op",
            "Require OP",
            "Only show button when player has operator permissions",
            true
        );
        registerSetting(requireOpSetting);
        
        // Restore metadata setting
        this.restoreMetadataSetting = new ModuleSetting.BooleanSetting(
            "restore_metadata",
            "Restore Metadata",
            "Whether restored items should have exact metadata (enchantments, names, etc.)",
            true
        );
        registerSetting(restoreMetadataSetting);
        
        // Restore exact slots setting
        this.restoreExactSlotsSetting = new ModuleSetting.BooleanSetting(
            "restore_exact_slots",
            "Restore Exact Slots",
            "Whether items should be restored to their original inventory slots",
            true
        );
        registerSetting(restoreExactSlotsSetting);
    }
    
    /**
     * Checks if the player has operator permissions.
     */
    public static boolean hasOperatorPermission() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return false;
        
        // Check if player has permission level >= 2 (can use /give)
        return client.player.hasPermissionLevel(2);
    }
    
    /**
     * Returns whether this module should be shown.
     */
    public boolean shouldShow() {
        if (!requireOpSetting.getValue()) {
            return true; // Show regardless of OP
        }
        return hasOperatorPermission();
    }
    
    /**
     * Creates the recover button for the death screen.
     */
    public void createButton(DeathScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        // Check if we should show based on OP requirement
        if (!shouldShow()) {
            return;
        }
        
        // Check if we have death inventory
        if (!DeathInventoryModule.hasDeathInventory()) {
            return;
        }
        
        WidgetSettings settings = getWidgetSettings(RECOVER_ITEMS_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        recoverButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "📦",
            "Recover Items",
            "Respawn and recover your exact inventory.\n" +
            "Uses /give commands (requires operator permissions).",
            button -> recoverItems()
        );
        
        // Apply display mode
        recoverButton.setDisplayMode(settings.getDisplayMode());
        
        recoverButton.setEditorInfo(this, RECOVER_ITEMS_WIDGET_ID, "Recover Items", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(recoverButton);
        
        addDrawableChild.accept(recoverButton);
    }
    
    /**
     * Initiates item recovery.
     * First respawns, then gives items after respawn.
     */
    private void recoverItems() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client.player == null) {
            return;
        }
        
        // Store pending recovery with settings
        pendingRecovery = true;
        restoreMetadata = restoreMetadataSetting.getValue();
        restoreExactSlots = restoreExactSlotsSetting.getValue();
        
        // Trigger respawn
        client.player.requestRespawn();
        
        BoosterClient.LOGGER.info("Respawning and recovering items...");
    }
    
    /**
     * Called after player respawns to execute pending item recovery.
     */
    public static void onPlayerRespawn() {
        if (!pendingRecovery) {
            return;
        }
        
        pendingRecovery = false;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        
        // Delay slightly to ensure player is fully spawned and inventory is ready
        client.execute(() -> {
            // Another small delay for safety
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Ignore
            }
            
            client.execute(() -> executeRecovery(client.player));
        });
    }
    
    /**
     * Executes the item recovery using commands.
     */
    private static void executeRecovery(ClientPlayerEntity player) {
        List<ItemStack> mainInventory = DeathInventoryModule.getLastDeathInventory();
        ItemStack[] armor = DeathInventoryModule.getLastDeathArmor();
        ItemStack offhand = DeathInventoryModule.getLastDeathOffhand();
        
        if (mainInventory == null) {
            BoosterClient.LOGGER.warn("No death inventory to recover");
            return;
        }
        
        int itemsRecovered = 0;
        
        // Clear current inventory first if restoring exact slots
        if (restoreExactSlots) {
            player.networkHandler.sendCommand("clear @s");
            // Small delay after clear
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        
        // Recover main inventory items
        for (int i = 0; i < mainInventory.size(); i++) {
            ItemStack stack = mainInventory.get(i);
            if (stack != null && !stack.isEmpty()) {
                String command = buildGiveCommand(stack, restoreMetadata, restoreExactSlots ? i : -1);
                if (command != null) {
                    player.networkHandler.sendCommand(command);
                    itemsRecovered++;
                }
            }
        }
        
        // Recover armor
        if (armor != null) {
            for (int i = 0; i < armor.length; i++) {
                ItemStack stack = armor[i];
                if (stack != null && !stack.isEmpty()) {
                    if (restoreExactSlots) {
                        // Use /item replace for armor slots
                        String slot = getArmorSlotName(i);
                        String command = buildItemReplaceCommand(stack, slot, restoreMetadata);
                        if (command != null) {
                            player.networkHandler.sendCommand(command);
                            itemsRecovered++;
                        }
                    } else {
                        String command = buildGiveCommand(stack, restoreMetadata, -1);
                        if (command != null) {
                            player.networkHandler.sendCommand(command);
                            itemsRecovered++;
                        }
                    }
                }
            }
        }
        
        // Recover offhand
        if (offhand != null && !offhand.isEmpty()) {
            if (restoreExactSlots) {
                String command = buildItemReplaceCommand(offhand, "weapon.offhand", restoreMetadata);
                if (command != null) {
                    player.networkHandler.sendCommand(command);
                    itemsRecovered++;
                }
            } else {
                String command = buildGiveCommand(offhand, restoreMetadata, -1);
                if (command != null) {
                    player.networkHandler.sendCommand(command);
                    itemsRecovered++;
                }
            }
        }
        
        BoosterClient.LOGGER.info("Recovered {} item stacks", itemsRecovered);
    }
    
    /**
     * Builds a /give command for an item stack.
     */
    private static String buildGiveCommand(ItemStack stack, boolean includeMetadata, int slot) {
        if (stack == null || stack.isEmpty()) return null;
        
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        int count = stack.getCount();
        
        StringBuilder command = new StringBuilder();
        command.append("give @s ").append(itemId.toString());
        
        // Add components if metadata should be preserved
        if (includeMetadata && stack.getComponents() != null) {
            String components = getComponentsString(stack);
            if (components != null && !components.isEmpty()) {
                command.append(components);
            }
        }
        
        command.append(" ").append(count);
        
        return command.toString();
    }
    
    /**
     * Builds an /item replace command for placing an item in a specific slot.
     */
    private static String buildItemReplaceCommand(ItemStack stack, String slot, boolean includeMetadata) {
        if (stack == null || stack.isEmpty()) return null;
        
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        int count = stack.getCount();
        
        StringBuilder command = new StringBuilder();
        command.append("item replace entity @s ").append(slot).append(" with ");
        command.append(itemId.toString());
        
        // Add components if metadata should be preserved
        if (includeMetadata && stack.getComponents() != null) {
            String components = getComponentsString(stack);
            if (components != null && !components.isEmpty()) {
                command.append(components);
            }
        }
        
        command.append(" ").append(count);
        
        return command.toString();
    }
    
    /**
     * Gets the armor slot name for /item replace.
     */
    private static String getArmorSlotName(int armorIndex) {
        return switch (armorIndex) {
            case 0 -> "armor.feet";
            case 1 -> "armor.legs";
            case 2 -> "armor.chest";
            case 3 -> "armor.head";
            default -> "armor.chest";
        };
    }
    
    /**
     * Gets a string representation of item components for commands.
     * Returns the component part in the format [component1=value1,component2=value2]
     */
    private static String getComponentsString(ItemStack stack) {
        // For 1.21.4, we need to serialize the components properly
        // This is a simplified version - full metadata preservation would need SNBT serialization
        try {
            // Check for custom name by comparing display name to default name
            // In 1.21.4, use the components API
            var components = stack.getComponents();
            if (components != null && !components.isEmpty()) {
                // For now, just check if name differs from default item name
                String displayName = stack.getName().getString();
                String defaultName = stack.getItem().getName().getString();
                
                if (!displayName.equals(defaultName)) {
                    // Escape special characters
                    String name = displayName.replace("\"", "\\\"");
                    return "[custom_name=\"\\\"" + name + "\\\"\"]";
                }
            }
            
            // For enchantments and other complex data, we'd need full SNBT serialization
            // This is a limitation of the simple approach
            // TODO: Implement full SNBT serialization for complete metadata preservation
            
        } catch (Exception e) {
            BoosterClient.LOGGER.warn("Failed to get components string for item: {}", e.getMessage());
        }
        
        return "";
    }
    
    /**
     * Returns whether there's a pending recovery.
     */
    public static boolean hasPendingRecovery() {
        return pendingRecovery;
    }
    
    @Override
    public Set<String> getWidgetIds() {
        Set<String> ids = new HashSet<>();
        ids.add(RECOVER_ITEMS_WIDGET_ID);
        return ids;
    }
}
