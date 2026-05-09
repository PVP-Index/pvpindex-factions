package com.pvpindex.factions.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed wrapper for gui.yml.
 */
public class GuiConfig {

    private final FileConfiguration cfg;

    public GuiConfig(final FileConfiguration cfg) {
        this.cfg = cfg;
    }

    public FileConfiguration raw() {
        return cfg;
    }

    public boolean isEnabled() {
        return cfg.getBoolean("gui.enabled", true);
    }

    public String getDefaultMenu() {
        return cfg.getString("gui.default-menu", "main");
    }
}
