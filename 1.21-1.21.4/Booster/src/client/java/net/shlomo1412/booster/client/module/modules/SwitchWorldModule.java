package net.shlomo1412.booster.client.module.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.shlomo1412.booster.client.BoosterClient;
import net.shlomo1412.booster.client.editor.EditorModeManager;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.ModuleSetting;
import net.shlomo1412.booster.client.module.WidgetSettings;
import net.shlomo1412.booster.client.widget.BoosterButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Module that adds a "Switch World" dropdown button to the singleplayer pause menu.
 * Shows a list of recent worlds that can be switched to instantly.
 */
public class SwitchWorldModule extends GUIModule {
    
    public static final String SWITCH_WORLD_WIDGET_ID = "switch_world";
    
    private final ModuleSetting.NumberSetting maxWorldsSetting;
    
    private BoosterButton switchWorldButton;
    private boolean dropdownOpen = false;
    private List<WorldEntry> recentWorlds = new ArrayList<>();
    private Screen parentScreen;
    
    public SwitchWorldModule() {
        super(
            "switch_world",
            "Switch World",
            "Adds a dropdown to quickly switch to another world.\n" +
            "Shows recent worlds for instant switching.\n" +
            "Singleplayer only.",
            true,
            20,  // Default button width
            20   // Default button height
        );
        
        // Configurable number of recent worlds to show
        this.maxWorldsSetting = new ModuleSetting.NumberSetting(
            "max_worlds",
            "Max Worlds",
            "Maximum number of recent worlds to show in the dropdown",
            5,   // default
            1,   // min
            15   // max
        );
        registerSetting(maxWorldsSetting);
    }
    
    /**
     * Creates the Switch World button for the pause menu.
     */
    public void createButton(GameMenuScreen screen, int anchorX, int anchorY,
                            Consumer<BoosterButton> addDrawableChild) {
        
        this.parentScreen = screen;
        
        WidgetSettings settings = getWidgetSettings(SWITCH_WORLD_WIDGET_ID, 0, 0);
        
        int buttonX = anchorX + settings.getOffsetX();
        int buttonY = anchorY + settings.getOffsetY();
        
        switchWorldButton = new BoosterButton(
            buttonX, buttonY,
            settings.getWidth(), settings.getHeight(),
            "🔄",
            "Switch",
            "Click to show recent worlds.\n" +
            "Quickly switch to another world.",
            button -> toggleDropdown()
        );
        
        // Apply display mode from settings
        switchWorldButton.setDisplayMode(settings.getDisplayMode());
        
        switchWorldButton.setEditorInfo(this, SWITCH_WORLD_WIDGET_ID, "Switch World", anchorX, anchorY);
        EditorModeManager.getInstance().registerDraggableWidget(switchWorldButton);
        
        addDrawableChild.accept(switchWorldButton);
        
        // Load recent worlds
        loadRecentWorlds();
    }
    
    /**
     * Toggles the dropdown visibility.
     */
    private void toggleDropdown() {
        dropdownOpen = !dropdownOpen;
        if (dropdownOpen) {
            loadRecentWorlds();
        }
    }
    
    /**
     * Loads the list of recent worlds.
     */
    private void loadRecentWorlds() {
        MinecraftClient client = MinecraftClient.getInstance();
        recentWorlds.clear();
        
        try {
            LevelStorage levelStorage = client.getLevelStorage();
            CompletableFuture<List<LevelSummary>> future = levelStorage.loadSummaries(levelStorage.getLevelList());
            
            future.thenAccept(summaries -> {
                // Get current world name to exclude it
                String currentWorldFolder = null;
                if (client.getServer() != null) {
                    currentWorldFolder = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                        .getParent().getFileName().toString();
                }
                
                final String currentWorld = currentWorldFolder;
                
                // Sort by last played and take top N
                List<LevelSummary> sorted = summaries.stream()
                    .filter(s -> !s.getName().equals(currentWorld)) // Exclude current world
                    .sorted(Comparator.comparingLong(LevelSummary::getLastPlayed).reversed())
                    .limit(maxWorldsSetting.getValue())
                    .toList();
                
                recentWorlds.clear();
                for (LevelSummary summary : sorted) {
                    recentWorlds.add(new WorldEntry(
                        summary.getName(),
                        summary.getDisplayName(),
                        summary.getLastPlayed()
                    ));
                }
            });
        } catch (Exception e) {
            BoosterClient.LOGGER.error("Failed to load world list", e);
        }
    }
    
    /**
     * Switches to the specified world.
     */
    private void switchToWorld(WorldEntry world) {
        MinecraftClient client = MinecraftClient.getInstance();
        dropdownOpen = false;
        
        // First save and quit current world, then load new one
        if (client.isInSingleplayer() && client.world != null) {
            // Disconnect from current world
            client.world.disconnect();
            client.disconnect();
            
            // Load the new world
            client.createIntegratedServerLoader().start(world.folderName, () -> {
                client.setScreen(new SelectWorldScreen(new net.minecraft.client.gui.screen.TitleScreen()));
            });
        }
    }
    
    /**
     * Renders the dropdown if open.
     */
    public void renderDropdown(DrawContext context, int mouseX, int mouseY) {
        if (!dropdownOpen || switchWorldButton == null) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        int x = switchWorldButton.getX();
        int y = switchWorldButton.getY() + switchWorldButton.getHeight() + 2;
        int width = 150;
        int itemHeight = 20;
        
        if (recentWorlds.isEmpty()) {
            // Show "No worlds" message
            context.fill(x, y, x + width, y + itemHeight, 0xE0202020);
            context.drawBorder(x, y, width, itemHeight, 0xFF404040);
            context.drawCenteredTextWithShadow(client.textRenderer, "No other worlds", x + width/2, y + 6, 0x888888);
            return;
        }
        
        int totalHeight = recentWorlds.size() * itemHeight;
        
        // Background
        context.fill(x, y, x + width, y + totalHeight, 0xE0202020);
        context.drawBorder(x, y, width, totalHeight, 0xFF404040);
        
        // Render each world entry
        for (int i = 0; i < recentWorlds.size(); i++) {
            WorldEntry world = recentWorlds.get(i);
            int itemY = y + i * itemHeight;
            
            // Highlight on hover
            boolean hovered = mouseX >= x && mouseX < x + width && 
                             mouseY >= itemY && mouseY < itemY + itemHeight;
            
            if (hovered) {
                context.fill(x + 1, itemY, x + width - 1, itemY + itemHeight, 0x60FFFFFF);
            }
            
            // World name (truncated if too long)
            String displayName = world.displayName;
            if (client.textRenderer.getWidth(displayName) > width - 10) {
                while (client.textRenderer.getWidth(displayName + "...") > width - 10 && displayName.length() > 1) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                displayName += "...";
            }
            
            context.drawTextWithShadow(client.textRenderer, displayName, x + 5, itemY + 6, 0xFFFFFF);
        }
    }
    
    /**
     * Handles mouse click on the dropdown.
     * @return true if the click was handled
     */
    public boolean handleDropdownClick(double mouseX, double mouseY) {
        if (!dropdownOpen || switchWorldButton == null) return false;
        
        int x = switchWorldButton.getX();
        int y = switchWorldButton.getY() + switchWorldButton.getHeight() + 2;
        int width = 150;
        int itemHeight = 20;
        
        for (int i = 0; i < recentWorlds.size(); i++) {
            int itemY = y + i * itemHeight;
            
            if (mouseX >= x && mouseX < x + width && 
                mouseY >= itemY && mouseY < itemY + itemHeight) {
                switchToWorld(recentWorlds.get(i));
                return true;
            }
        }
        
        // Click outside dropdown closes it
        dropdownOpen = false;
        return false;
    }
    
    /**
     * @return Whether the dropdown is currently open
     */
    public boolean isDropdownOpen() {
        return dropdownOpen;
    }
    
    /**
     * Closes the dropdown.
     */
    public void closeDropdown() {
        dropdownOpen = false;
    }
    
    /**
     * Gets the button.
     */
    public BoosterButton getButton() {
        return switchWorldButton;
    }
    
    /**
     * Clears button reference when screen closes.
     */
    public void clearButton() {
        switchWorldButton = null;
        dropdownOpen = false;
        parentScreen = null;
    }
    
    /**
     * Simple record for world entries.
     */
    private record WorldEntry(String folderName, String displayName, long lastPlayed) {}
}
