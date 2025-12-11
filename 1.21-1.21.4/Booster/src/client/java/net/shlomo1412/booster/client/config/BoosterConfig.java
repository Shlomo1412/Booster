package net.shlomo1412.booster.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.Module;
import net.shlomo1412.booster.client.module.ModuleSetting;
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
    
    // Last server tracking
    private String lastServerName = null;
    private String lastServerAddress = null;
    
    // Last world tracking
    private String lastWorldName = null;
    private String lastWorldDisplayName = null;

    public BoosterConfig() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }
    
    // ==================== Editor Guide ====================
    
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
    
    // ==================== Last Server ====================
    
    public String getLastServerName() {
        return lastServerName;
    }
    
    public void setLastServerName(String name) {
        this.lastServerName = name;
    }
    
    public String getLastServerAddress() {
        return lastServerAddress;
    }
    
    public void setLastServerAddress(String address) {
        this.lastServerAddress = address;
    }
    
    // ==================== Last World ====================
    
    public String getLastWorldName() {
        return lastWorldName;
    }
    
    public void setLastWorldName(String name) {
        this.lastWorldName = name;
    }
    
    public String getLastWorldDisplayName() {
        return lastWorldDisplayName;
    }
    
    public void setLastWorldDisplayName(String displayName) {
        this.lastWorldDisplayName = displayName;
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
        
        // Save last server
        if (lastServerName != null) settings.addProperty("lastServerName", lastServerName);
        if (lastServerAddress != null) settings.addProperty("lastServerAddress", lastServerAddress);
        
        // Save last world
        if (lastWorldName != null) settings.addProperty("lastWorldName", lastWorldName);
        if (lastWorldDisplayName != null) settings.addProperty("lastWorldDisplayName", lastWorldDisplayName);
        
        root.add("settings", settings);
        
        JsonObject modulesObject = new JsonObject();

        for (Module module : modules) {
            JsonObject moduleData = new JsonObject();
            moduleData.addProperty("enabled", module.isEnabled());

            // Save GUI module per-widget settings and module settings
            if (module instanceof GUIModule guiModule) {
                // Save widget settings
                JsonObject widgetsObject = new JsonObject();
                
                for (Map.Entry<String, WidgetSettings> entry : guiModule.getAllWidgetSettings().entrySet()) {
                    String widgetId = entry.getKey();
                    WidgetSettings widgetSettings = entry.getValue();
                    
                    JsonObject widgetData = new JsonObject();
                    widgetData.addProperty("offsetX", widgetSettings.getOffsetX());
                    widgetData.addProperty("offsetY", widgetSettings.getOffsetY());
                    widgetData.addProperty("width", widgetSettings.getWidth());
                    widgetData.addProperty("height", widgetSettings.getHeight());
                    widgetData.addProperty("displayMode", widgetSettings.getDisplayMode().name());
                    
                    widgetsObject.add(widgetId, widgetData);
                }
                
                moduleData.add("widgets", widgetsObject);
                
                // Save module settings (colors, enums, etc.)
                if (guiModule.hasSettings()) {
                    JsonObject moduleSettingsObject = new JsonObject();
                    
                    for (ModuleSetting<?> setting : guiModule.getSettings()) {
                        saveModuleSetting(moduleSettingsObject, setting);
                    }
                    
                    moduleData.add("moduleSettings", moduleSettingsObject);
                }
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
     * Saves a single module setting to JSON.
     */
    private void saveModuleSetting(JsonObject parent, ModuleSetting<?> setting) {
        switch (setting.getType()) {
            case COLOR -> {
                ModuleSetting.ColorSetting colorSetting = (ModuleSetting.ColorSetting) setting;
                parent.addProperty(setting.getId(), colorSetting.getValue());
            }
            case ENUM -> {
                @SuppressWarnings("unchecked")
                ModuleSetting.EnumSetting<? extends Enum<?>> enumSetting = 
                    (ModuleSetting.EnumSetting<? extends Enum<?>>) setting;
                parent.addProperty(setting.getId(), enumSetting.getValue().name());
            }
            case NUMBER -> {
                ModuleSetting.NumberSetting numberSetting = (ModuleSetting.NumberSetting) setting;
                parent.addProperty(setting.getId(), numberSetting.getValue());
            }
            case BOOLEAN -> {
                ModuleSetting.BooleanSetting boolSetting = (ModuleSetting.BooleanSetting) setting;
                parent.addProperty(setting.getId(), boolSetting.getValue());
            }
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
                
                // Load last server
                if (settings.has("lastServerName")) {
                    lastServerName = settings.get("lastServerName").getAsString();
                }
                if (settings.has("lastServerAddress")) {
                    lastServerAddress = settings.get("lastServerAddress").getAsString();
                }
                
                // Load last world
                if (settings.has("lastWorldName")) {
                    lastWorldName = settings.get("lastWorldName").getAsString();
                }
                if (settings.has("lastWorldDisplayName")) {
                    lastWorldDisplayName = settings.get("lastWorldDisplayName").getAsString();
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

                // Load GUI module per-widget settings and module settings
                if (module instanceof GUIModule guiModule) {
                    // Load widget settings
                    if (moduleData.has("widgets")) {
                        JsonObject widgetsObject = moduleData.getAsJsonObject("widgets");
                        
                        for (String widgetId : widgetsObject.keySet()) {
                            JsonObject widgetData = widgetsObject.getAsJsonObject(widgetId);
                            
                            int offsetX = widgetData.has("offsetX") ? widgetData.get("offsetX").getAsInt() : 0;
                            int offsetY = widgetData.has("offsetY") ? widgetData.get("offsetY").getAsInt() : 0;
                            int width = widgetData.has("width") ? widgetData.get("width").getAsInt() : guiModule.getDefaultWidth();
                            int height = widgetData.has("height") ? widgetData.get("height").getAsInt() : guiModule.getDefaultHeight();
                            
                            // Load display mode
                            net.shlomo1412.booster.client.widget.ButtonDisplayMode displayMode = 
                                net.shlomo1412.booster.client.widget.ButtonDisplayMode.AUTO;
                            if (widgetData.has("displayMode")) {
                                try {
                                    displayMode = net.shlomo1412.booster.client.widget.ButtonDisplayMode.valueOf(
                                        widgetData.get("displayMode").getAsString());
                                } catch (IllegalArgumentException ignored) {}
                            }
                            
                            // Load widget settings - actual defaults will be set when createButtons is called
                            guiModule.loadWidgetSettings(widgetId, offsetX, offsetY, width, height, displayMode);
                        }
                    }
                    
                    // Load module settings (colors, enums, etc.)
                    if (moduleData.has("moduleSettings")) {
                        JsonObject moduleSettingsObject = moduleData.getAsJsonObject("moduleSettings");
                        
                        for (ModuleSetting<?> setting : guiModule.getSettings()) {
                            loadModuleSetting(moduleSettingsObject, setting);
                        }
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
     * Loads a single module setting from JSON.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void loadModuleSetting(JsonObject parent, ModuleSetting<?> setting) {
        if (!parent.has(setting.getId())) {
            return;
        }
        
        try {
            switch (setting.getType()) {
                case COLOR -> {
                    ModuleSetting.ColorSetting colorSetting = (ModuleSetting.ColorSetting) setting;
                    colorSetting.setValue(parent.get(setting.getId()).getAsInt());
                }
                case ENUM -> {
                    ModuleSetting.EnumSetting enumSetting = (ModuleSetting.EnumSetting) setting;
                    String enumName = parent.get(setting.getId()).getAsString();
                    try {
                        Enum<?> enumValue = Enum.valueOf(enumSetting.getEnumClass(), enumName);
                        enumSetting.setValue(enumValue);
                    } catch (IllegalArgumentException e) {
                        // Keep default if enum value not found
                    }
                }
                case NUMBER -> {
                    ModuleSetting.NumberSetting numberSetting = (ModuleSetting.NumberSetting) setting;
                    numberSetting.setValue(parent.get(setting.getId()).getAsInt());
                }
                case BOOLEAN -> {
                    ModuleSetting.BooleanSetting boolSetting = (ModuleSetting.BooleanSetting) setting;
                    boolSetting.setValue(parent.get(setting.getId()).getAsBoolean());
                }
            }
        } catch (Exception e) {
            // Keep default value on parse error
            System.err.println("[Booster] Failed to load setting '" + setting.getId() + "': " + e.getMessage());
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
