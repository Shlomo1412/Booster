package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.BlockPos;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Module that adds an Add Coordinates button to the chat screen.
 * Inserts the player's current coordinates into the chat input field.
 */
public class AddCoordsModule extends GUIModule {
    
    public static final String ADD_COORDS_WIDGET_ID = "add_coords";
    
    // Settings
    private final ModuleSetting.BooleanSetting includeYSetting;
    private final ModuleSetting.BooleanSetting includeDimensionSetting;
    
    private BoosterButton addCoordsButton;
    private Consumer<String> insertTextAction;
    
    public AddCoordsModule() {
        super(
            "add_coords",
            "Add Coordinates",
            "Adds a button to insert your current coordinates into the chat input field.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Include Y coordinate setting
        this.includeYSetting = new ModuleSetting.BooleanSetting(
            "include_y",
            "Include Y",
            "Include the Y coordinate (height) in the output",
            true
        );
        registerSetting(includeYSetting);
        
        // Include dimension setting
        this.includeDimensionSetting = new ModuleSetting.BooleanSetting(
            "include_dimension",
            "Include Dimension",
            "Include the dimension name (Overworld, Nether, End)",
            false
        );
        registerSetting(includeDimensionSetting);
    }
    
    /**
     * Creates the add coordinates button for the chat screen.
     *
     * @param screen The chat screen
     * @param anchorX The anchor X position
     * @param anchorY The anchor Y position
     * @param insertTextAction Action to insert text into the chat input
     * @param addDrawableChild Callback to add the button
     */
    public void createButton(ChatScreen screen, int anchorX, int anchorY, 
                             Consumer<String> insertTextAction, Consumer<BoosterButton> addDrawableChild) {
        this.insertTextAction = insertTextAction;
        
        // Get per-widget settings
        WidgetSettings settings = getWidgetSettings(ADD_COORDS_WIDGET_ID, 48, 0);
        
        // Calculate button position
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        addCoordsButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "📍",  // Pin/location icon
            "Add Coords",
            "Inserts your current coordinates into the chat input.",
            button -> addCoordinates()
        );
        
        // Skip keyboard navigation to avoid interfering with chat input
        addCoordsButton.setSkipKeyboardNavigation(true);
        
        // Apply display mode from settings
        addCoordsButton.setDisplayMode(settings.getDisplayMode());
        
        // Set editor info
        addCoordsButton.setEditorInfo(this, ADD_COORDS_WIDGET_ID, "Add Coords", anchorX, anchorY);
        
        addDrawableChild.accept(addCoordsButton);
    }
    
    /**
     * Gets the buttons created by this module.
     */
    public List<BoosterButton> getButtons() {
        List<BoosterButton> buttons = new ArrayList<>();
        if (addCoordsButton != null) buttons.add(addCoordsButton);
        return buttons;
    }
    
    /**
     * Adds the player's coordinates to the chat input.
     */
    private void addCoordinates() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || insertTextAction == null) return;
        
        BlockPos pos = client.player.getBlockPos();
        StringBuilder coords = new StringBuilder();
        
        // Build coordinate string
        if (includeYSetting.getValue()) {
            coords.append(String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ()));
        } else {
            coords.append(String.format("X: %d, Z: %d", pos.getX(), pos.getZ()));
        }
        
        // Add dimension if enabled
        if (includeDimensionSetting.getValue() && client.world != null) {
            String dimension = getDimensionName(client.world.getRegistryKey().getValue().getPath());
            coords.append(" (").append(dimension).append(")");
        }
        
        insertTextAction.accept(coords.toString());
    }
    
    /**
     * Gets a friendly name for the dimension.
     */
    private String getDimensionName(String dimensionPath) {
        return switch (dimensionPath) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> dimensionPath;
        };
    }
    
    @Override
    protected void onEnable() {
        // Nothing special to do on enable
    }
    
    @Override
    protected void onDisable() {
        addCoordsButton = null;
        insertTextAction = null;
    }
}
