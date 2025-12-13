package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.util.math.BlockPos;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.function.Consumer;

/**
 * Module that displays death coordinates on the death screen.
 * Shows X, Y, Z coordinates and dimension where the player died.
 */
public class DeathCoordinatesModule extends GUIModule {
    
    public static final String DEATH_COORDS_WIDGET_ID = "death_coordinates";
    
    // Static storage for death location (set when player dies)
    private static BlockPos lastDeathPos = null;
    private static String lastDeathDimension = null;
    
    private final ModuleSetting.ColorSetting textColorSetting;
    private final ModuleSetting.BooleanSetting showDimensionSetting;
    private final ModuleSetting.BooleanSetting showBackgroundSetting;
    
    private BoosterButton coordsDisplay;
    
    public DeathCoordinatesModule() {
        super(
            "death_coordinates",
            "Death Coordinates",
            "Displays your death coordinates on the death screen.\n" +
            "Shows X, Y, Z and optionally the dimension.",
            true,
            150, // Default width
            20   // Default height
        );
        
        // Text color setting
        this.textColorSetting = new ModuleSetting.ColorSetting(
            "text_color",
            "Text Color",
            "Color of the coordinates text",
            0xFFFFFF55  // Yellow
        );
        registerSetting(textColorSetting);
        
        // Show dimension setting
        this.showDimensionSetting = new ModuleSetting.BooleanSetting(
            "show_dimension",
            "Show Dimension",
            "Whether to show the dimension name",
            true
        );
        registerSetting(showDimensionSetting);
        
        // Show background setting
        this.showBackgroundSetting = new ModuleSetting.BooleanSetting(
            "show_background",
            "Show Background",
            "Whether to show a background behind the text",
            true
        );
        registerSetting(showBackgroundSetting);
    }
    
    /**
     * Records the player's death location.
     * Called when the player dies.
     */
    public static void recordDeathLocation(BlockPos pos, String dimension) {
        lastDeathPos = pos;
        lastDeathDimension = dimension;
    }
    
    /**
     * Gets the last death position.
     */
    public static BlockPos getLastDeathPos() {
        return lastDeathPos;
    }
    
    /**
     * Gets the last death dimension.
     */
    public static String getLastDeathDimension() {
        return lastDeathDimension;
    }
    
    /**
     * Checks if there's a recorded death location.
     */
    public static boolean hasDeathLocation() {
        return lastDeathPos != null;
    }
    
    /**
     * Creates the death coordinates display for the death screen.
     */
    public void createDisplay(DeathScreen screen, int anchorX, int anchorY,
                             Consumer<BoosterButton> addDrawableChild) {
        
        WidgetSettings settings = getWidgetSettings(DEATH_COORDS_WIDGET_ID, 0, 0);
        
        int displayX = anchorX + settings.getOffsetX();
        int displayY = anchorY + settings.getOffsetY();
        
        String coordsText = getCoordinatesText();
        
        coordsDisplay = new BoosterButton(
            displayX, displayY,
            settings.getWidth(), settings.getHeight(),
            "📍",
            coordsText,
            "Your death coordinates.\n" +
            "Click to copy to clipboard.",
            button -> copyToClipboard()
        );
        
        // Apply display mode
        coordsDisplay.setDisplayMode(settings.getDisplayMode());
        
        coordsDisplay.setEditorInfo(this, DEATH_COORDS_WIDGET_ID, "Death Coordinates", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(coordsDisplay);
        
        addDrawableChild.accept(coordsDisplay);
    }
    
    /**
     * Renders the coordinates text (alternative rendering without button).
     */
    public void renderCoordinates(DrawContext context, int anchorX, int anchorY, int mouseX, int mouseY) {
        if (!hasDeathLocation()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        WidgetSettings settings = getWidgetSettings(DEATH_COORDS_WIDGET_ID, 0, 0);
        
        int x = anchorX + settings.getOffsetX();
        int y = anchorY + settings.getOffsetY();
        
        String text = getCoordinatesText();
        int textWidth = client.textRenderer.getWidth(text);
        
        // Background
        if (showBackgroundSetting.getValue()) {
            context.fill(x - 4, y - 2, x + textWidth + 4, y + 12, 0xA0000000);
        }
        
        // Text
        context.drawTextWithShadow(client.textRenderer, text, x, y, textColorSetting.getValue());
    }
    
    /**
     * Gets the formatted coordinates text.
     */
    private String getCoordinatesText() {
        if (!hasDeathLocation()) {
            return "No death location";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("X: ").append(lastDeathPos.getX());
        sb.append(" Y: ").append(lastDeathPos.getY());
        sb.append(" Z: ").append(lastDeathPos.getZ());
        
        if (showDimensionSetting.getValue() && lastDeathDimension != null) {
            sb.append(" (").append(formatDimension(lastDeathDimension)).append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Copies coordinates to clipboard.
     */
    private void copyToClipboard() {
        if (!hasDeathLocation()) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        String text = String.format("%d %d %d", lastDeathPos.getX(), lastDeathPos.getY(), lastDeathPos.getZ());
        client.keyboard.setClipboard(text);
    }
    
    /**
     * Formats dimension name for display.
     */
    private String formatDimension(String dim) {
        if (dim == null) return "";
        String path = dim.contains(":") ? dim.split(":")[1] : dim;
        return switch (path) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "Nether";
            case "the_end" -> "The End";
            default -> path.replace("_", " ");
        };
    }
    
    /**
     * Gets the button.
     */
    public BoosterButton getButton() {
        return coordsDisplay;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        coordsDisplay = null;
    }
}
