package net.shlomo1412.booster.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.shlomo1412.booster.client.module.GUIModule;
import net.shlomo1412.booster.client.module.Module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Handles saving and loading of Booster configuration.
 * Uses JSON format for human-readable config files.
 */
public class BoosterConfig {
    private static final String CONFIG_FILE_NAME = "booster.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;

    public BoosterConfig() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }

    /**
     * Saves all module configurations to the config file.
     *
     * @param modules The collection of modules to save
     */
    public void save(Collection<Module> modules) {
        JsonObject root = new JsonObject();
        JsonObject modulesObject = new JsonObject();

        for (Module module : modules) {
            JsonObject moduleData = new JsonObject();
            moduleData.addProperty("enabled", module.isEnabled());

            // Save GUI module specific settings
            if (module instanceof GUIModule guiModule) {
                moduleData.addProperty("offsetX", guiModule.getOffsetX());
                moduleData.addProperty("offsetY", guiModule.getOffsetY());
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

                // Load GUI module specific settings
                if (module instanceof GUIModule guiModule) {
                    if (moduleData.has("offsetX") && moduleData.has("offsetY")) {
                        int offsetX = moduleData.get("offsetX").getAsInt();
                        int offsetY = moduleData.get("offsetY").getAsInt();
                        loadOffset(guiModule, offsetX, offsetY);
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
     * Loads offset without triggering save.
     * Uses reflection to set the fields directly.
     */
    private void loadOffset(GUIModule module, int offsetX, int offsetY) {
        try {
            var fieldX = GUIModule.class.getDeclaredField("offsetX");
            var fieldY = GUIModule.class.getDeclaredField("offsetY");
            fieldX.setAccessible(true);
            fieldY.setAccessible(true);
            fieldX.set(module, offsetX);
            fieldY.set(module, offsetY);
        } catch (Exception e) {
            // Fallback: use the setter (will trigger save)
            module.setOffset(offsetX, offsetY);
        }
    }

    /**
     * @return The path to the config file
     */
    public Path getConfigPath() {
        return configPath;
    }
}
