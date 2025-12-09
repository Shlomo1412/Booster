package net.shlomo1412.booster.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.Module;
import net.shlomo1412.booster.client.module.WidgetSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * Handles saving and loading of Booster configuration.
 * Uses JSON format for human-readable config files.
 */
public class BoosterConfig {
    private static final String CONFIG_FILE_NAME = "booster.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;
    
    // Track if editor guide has been shown
    private boolean editorGuideShown = false;

    public BoosterConfig() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }
    
    /**
     * @return true if the editor guide has been shown before
     */
    public boolean hasEditorGuideBeenShown() {
        return editorGuideShown;
    }
    
    /**
     * Marks the editor guide as shown and saves config.
     */
    public void markEditorGuideShown() {
        this.editorGuideShown = true;
    }

    /**
     * Saves all module configurations to the config file.
     *
     * @param modules The collection of modules to save
     */
    public void save(Collection<Module> modules) {
        JsonObject root = new JsonObject();
        
        // Save general settings
        JsonObject settings = new JsonObject();
        settings.addProperty("editorGuideShown", editorGuideShown);
        root.add("settings", settings);
        
        JsonObject modulesObject = new JsonObject();

        for (Module module : modules) {
            JsonObject moduleData = new JsonObject();
            moduleData.addProperty("enabled", module.isEnabled());

            // Save GUI module per-widget settings
            if (module instanceof GUIModule guiModule) {
                JsonObject widgetsObject = new JsonObject();
                
                for (Map.Entry<String, WidgetSettings> entry : guiModule.getAllWidgetSettings().entrySet()) {
                    String widgetId = entry.getKey();
                    WidgetSettings widgetSettings = entry.getValue();
                    
                    JsonObject widgetData = new JsonObject();
                    widgetData.addProperty("offsetX", widgetSettings.getOffsetX());
                    widgetData.addProperty("offsetY", widgetSettings.getOffsetY());
                    widgetData.addProperty("width", widgetSettings.getWidth());
                    widgetData.addProperty("height", widgetSettings.getHeight());
                    
                    widgetsObject.add(widgetId, widgetData);
                }
                
                moduleData.add("widgets", widgetsObject);
            }

            modulesObject.add(module.getId(), moduleData);
        }

        root.add("modules", modulesObject);

        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, GSON.toJson(root));
        } catch (IOException e) {
            System.err.println("[Booster] Failed to save config: " + e.getMessage());
        }
    }

    /**
     * Loads module configurations from the config file.
     *
     * @param modules The collection of modules to load settings into
     */
    public void load(Collection<Module> modules) {
        if (!Files.exists(configPath)) {
            return;
        }

        try {
            String content = Files.readString(configPath);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            // Load general settings
            if (root.has("settings")) {
                JsonObject settings = root.getAsJsonObject("settings");
                if (settings.has("editorGuideShown")) {
                    editorGuideShown = settings.get("editorGuideShown").getAsBoolean();
                }
            }

            if (!root.has("modules")) {
                return;
            }

            JsonObject modulesObject = root.getAsJsonObject("modules");

            for (Module module : modules) {
                if (!modulesObject.has(module.getId())) {
                    continue;
                }

                JsonObject moduleData = modulesObject.getAsJsonObject(module.getId());

                // Load enabled state without triggering save
                if (moduleData.has("enabled")) {
                    boolean enabled = moduleData.get("enabled").getAsBoolean();
                    loadEnabledState(module, enabled);
                }

                // Load GUI module per-widget settings
                if (module instanceof GUIModule guiModule && moduleData.has("widgets")) {
                    JsonObject widgetsObject = moduleData.getAsJsonObject("widgets");
                    
                    for (String widgetId : widgetsObject.keySet()) {
                        JsonObject widgetData = widgetsObject.getAsJsonObject(widgetId);
                        
                        int offsetX = widgetData.has("offsetX") ? widgetData.get("offsetX").getAsInt() : 0;
                        int offsetY = widgetData.has("offsetY") ? widgetData.get("offsetY").getAsInt() : 0;
                        int width = widgetData.has("width") ? widgetData.get("width").getAsInt() : guiModule.getDefaultWidth();
                        int height = widgetData.has("height") ? widgetData.get("height").getAsInt() : guiModule.getDefaultHeight();
                        
                        // Load widget settings - actual defaults will be set when createButtons is called
                        guiModule.loadWidgetSettings(widgetId, offsetX, offsetY, width, height);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Booster] Failed to load config: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Booster] Failed to parse config: " + e.getMessage());
        }
    }

    /**
     * Loads enabled state without triggering callbacks/saves.
     * Uses reflection to set the field directly.
     */
    private void loadEnabledState(Module module, boolean enabled) {
        try {
            var field = Module.class.getDeclaredField("enabled");
            field.setAccessible(true);
            field.set(module, enabled);
        } catch (Exception e) {
            // Fallback: use the setter (will trigger save)
            module.setEnabled(enabled);
        }
    }

    /**
     * @return The path to the config file
     */
    public Path getConfigPath() {
        return configPath;
    }
}
