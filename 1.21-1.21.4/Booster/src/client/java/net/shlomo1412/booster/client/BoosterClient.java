package net.shlomo1412.booster.client;

import net.fabricmc.api.ClientModInitializer;
import net.shlomo1412.booster.client.editor.ScreenEditorHandler;
import net.shlomo1412.booster.client.module.ModuleManager;
import net.shlomo1412.booster.client.module.modules.AutoArmorModule;
import net.shlomo1412.booster.client.module.modules.ClearFurnaceModule;
import net.shlomo1412.booster.client.module.modules.ClearGridModule;
import net.shlomo1412.booster.client.module.modules.ConnectToServerModule;
import net.shlomo1412.booster.client.module.modules.CopyIPModule;
import net.shlomo1412.booster.client.module.modules.DatapacksFolderModule;
import net.shlomo1412.booster.client.module.modules.DeathCoordinatesModule;
import net.shlomo1412.booster.client.module.modules.DeathInventoryModule;
import net.shlomo1412.booster.client.module.modules.EstimatedFuelTimeModule;
import net.shlomo1412.booster.client.module.modules.HighlightFuelModule;
import net.shlomo1412.booster.client.module.modules.InfiniteCraftModule;
import net.shlomo1412.booster.client.module.modules.InventoryProgressModule;
import net.shlomo1412.booster.client.module.modules.LastServerModule;
import net.shlomo1412.booster.client.module.modules.LastWorldModule;
import net.shlomo1412.booster.client.module.modules.OpenScreenshotsModule;
import net.shlomo1412.booster.client.module.modules.OpenWorldFolderModule;
import net.shlomo1412.booster.client.module.modules.PinEstimatedTimeModule;
import net.shlomo1412.booster.client.module.modules.ReconnectModule;
import net.shlomo1412.booster.client.module.modules.RecoverItemsModule;
import net.shlomo1412.booster.client.module.modules.SaveQuitGameModule;
import net.shlomo1412.booster.client.module.modules.SaveQuitToServersModule;
import net.shlomo1412.booster.client.module.modules.SaveQuitToWorldsModule;
import net.shlomo1412.booster.client.module.modules.SearchBarModule;
import net.shlomo1412.booster.client.module.modules.ServerInfoModule;
import net.shlomo1412.booster.client.module.modules.ShowInventoryModule;
import net.shlomo1412.booster.client.module.modules.SmartFuelModule;
import net.shlomo1412.booster.client.module.modules.SortContainerModule;
import net.shlomo1412.booster.client.module.modules.SortInventoryModule;
import net.shlomo1412.booster.client.module.modules.StealStoreModule;
import net.shlomo1412.booster.client.module.modules.SwitchWorldModule;
import net.shlomo1412.booster.client.module.modules.TeleportToDeathModule;
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
        
        // Initialize screen editor event handlers
        ScreenEditorHandler.init();
        
        LOGGER.info("Booster client initialized with {} modules", 
                ModuleManager.getInstance().getModules().size());
    }

    /**
     * Registers all Booster modules.
     * Add new modules here.
     */
    private void registerModules() {
        ModuleManager manager = ModuleManager.getInstance();
        
        // GUI Modules - Inventory Screens
        manager.register(new StealStoreModule());
        manager.register(new SearchBarModule());
        manager.register(new InventoryProgressModule());
        manager.register(new SortInventoryModule());
        manager.register(new SortContainerModule());
        
        // GUI Modules - Crafting Table Screen
        manager.register(new ClearGridModule());
        manager.register(new InfiniteCraftModule());
        
        // GUI Modules - Player Inventory Screen
        manager.register(new AutoArmorModule());
        
        // Title Screen Modules
        manager.register(new LastServerModule());
        manager.register(new LastWorldModule());
        
        // Multiplayer Screen Modules
        manager.register(new CopyIPModule());
        
        // Pause Menu Modules
        manager.register(new SaveQuitGameModule());
        manager.register(new SaveQuitToWorldsModule());
        manager.register(new SaveQuitToServersModule());
        manager.register(new OpenScreenshotsModule());
        
        // Pause Menu Modules - Singleplayer Only
        manager.register(new OpenWorldFolderModule());
        manager.register(new DatapacksFolderModule());
        manager.register(new SwitchWorldModule());
        
        // Pause Menu Modules - Multiplayer Only
        manager.register(new ReconnectModule());
        manager.register(new ServerInfoModule());
        manager.register(new ConnectToServerModule());
        
        // Death Screen Modules
        manager.register(new DeathCoordinatesModule());
        manager.register(new TeleportToDeathModule());
        manager.register(new DeathInventoryModule());
        manager.register(new RecoverItemsModule());
        
        // Furnace Screen Modules (Furnace, Blast Furnace, Smoker)
        manager.register(new EstimatedFuelTimeModule());
        manager.register(new PinEstimatedTimeModule());
        manager.register(new SmartFuelModule());
        manager.register(new HighlightFuelModule());
        manager.register(new ClearFurnaceModule());
        
        // HUD Modules (non-GUI, keybind-based)
        manager.register(new ShowInventoryModule());
        
        // Add more modules here as they are created
    }
}
