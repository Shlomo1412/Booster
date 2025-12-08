package net.shlomo1412.booster.client;

import net.fabricmc.api.ClientModInitializer;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.StealStoreModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoosterClient implements ClientModInitializer {
    public static final String MOD_ID = "booster";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Booster client...");
        
        // Register all modules
        registerModules();
        
        // Initialize the module manager (loads config and triggers onEnable)
        ModuleManager.getInstance().initialize();
        
        LOGGER.info("Booster client initialized with {} modules", 
                ModuleManager.getInstance().getModules().size());
    }

    /**
     * Registers all Booster modules.
     * Add new modules here.
     */
    private void registerModules() {
        ModuleManager manager = ModuleManager.getInstance();
        
        // GUI Modules
        manager.register(new StealStoreModule());
        
        // Add more modules here as they are created
    }
}
