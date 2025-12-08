package net.shlomo1412.booster.client.module;

import net.shlomo1412.booster.client.config.BoosterConfig;

import java.util.*;

/**
 * Manages all Booster modules.
 * Handles registration, retrieval, and configuration persistence.
 */
public class ModuleManager {
    private static ModuleManager instance;

    private final Map<String, Module> modules = new LinkedHashMap<>();
    private final Map<Class<? extends Module>, Module> modulesByClass = new HashMap<>();
    private final BoosterConfig config;
    private boolean initialized = false;

    private ModuleManager() {
        this.config = new BoosterConfig();
    }

    /**
     * @return The singleton instance of the module manager
     */
    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    /**
     * Registers a module with the manager.
     *
     * @param module The module to register
     * @param <T>    The module type
     * @return The registered module (for chaining)
     * @throws IllegalStateException if a module with the same ID is already registered
     */
    public <T extends Module> T register(T module) {
        if (modules.containsKey(module.getId())) {
            throw new IllegalStateException("Module with ID '" + module.getId() + "' is already registered!");
        }

        modules.put(module.getId(), module);
        modulesByClass.put(module.getClass(), module);
        module.onRegister();

        return module;
    }

    /**
     * Gets a module by its ID.
     *
     * @param id The module ID
     * @return The module, or null if not found
     */
    public Module getModule(String id) {
        return modules.get(id);
    }

    /**
     * Gets a module by its class.
     *
     * @param moduleClass The module class
     * @param <T>         The module type
     * @return The module, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> moduleClass) {
        return (T) modulesByClass.get(moduleClass);
    }

    /**
     * @return An unmodifiable collection of all registered modules
     */
    public Collection<Module> getModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    /**
     * Gets all modules of a specific type.
     *
     * @param type The module type class
     * @param <T>  The module type
     * @return A list of modules of the specified type
     */
    @SuppressWarnings("unchecked")
    public <T extends Module> List<T> getModulesOfType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Module module : modules.values()) {
            if (type.isInstance(module)) {
                result.add((T) module);
            }
        }
        return result;
    }

    /**
     * Saves all module configurations to the config file.
     */
    public void saveConfig() {
        if (initialized) {
            config.save(modules.values());
        }
    }

    /**
     * Loads module configurations from the config file.
     * Should be called after all modules are registered.
     */
    public void loadConfig() {
        config.load(modules.values());
        initialized = true;
    }

    /**
     * Initializes the module manager.
     * Call this after registering all modules.
     */
    public void initialize() {
        loadConfig();
        
        // Trigger onEnable for modules that are enabled
        for (Module module : modules.values()) {
            if (module.isEnabled()) {
                module.onEnable();
            }
        }
    }

    /**
     * @return Whether the module manager has been initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
}
