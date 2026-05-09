package com.pvpindex.factions.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed access wrapper for messages.yml.
 */
public class MessagesConfig {

    private final FileConfiguration cfg;

    public MessagesConfig(final FileConfiguration cfg) {
        this.cfg = cfg;
    }

    public String get(final String path, final String fallback) {
        return cfg.getString(path, fallback);
    }
}
