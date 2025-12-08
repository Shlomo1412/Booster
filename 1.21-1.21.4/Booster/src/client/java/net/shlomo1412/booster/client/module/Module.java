package net.shlomo1412.booster.client.module;

/**
 * Base class for all Booster modules.
 * A module represents a feature that can be toggled on/off and configured.
 */
public abstract class Module {
    private final String id;
    private final String name;
    private final String description;
    private boolean enabled;

    /**
     * Creates a new module.
     *
     * @param id          Unique identifier for this module (used for config saving)
     * @param name        Display name of the module
     * @param description Description of what this module does
     * @param defaultEnabled Whether this module is enabled by default
     */
    public Module(String id, String name, String description, boolean defaultEnabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = defaultEnabled;
    }

    /**
     * @return The unique identifier of this module
     */
    public String getId() {
        return id;
    }

    /**
     * @return The display name of this module
     */
    public String getName() {
        return name;
    }

    /**
     * @return The description of this module
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return Whether this module is currently enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether this module is enabled.
     *
     * @param enabled The new enabled state
     */
    public void setEnabled(boolean enabled) {
        boolean wasEnabled = this.enabled;
        this.enabled = enabled;
        
        if (wasEnabled != enabled) {
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
            ModuleManager.getInstance().saveConfig();
        }
    }

    /**
     * Toggles the enabled state of this module.
     */
    public void toggle() {
        setEnabled(!enabled);
    }

    /**
     * Called when the module is enabled.
     * Override to perform initialization.
     */
    protected void onEnable() {
    }

    /**
     * Called when the module is disabled.
     * Override to perform cleanup.
     */
    protected void onDisable() {
    }

    /**
     * Called when the module is first registered.
     * Override to perform one-time setup.
     */
    public void onRegister() {
    }
}
