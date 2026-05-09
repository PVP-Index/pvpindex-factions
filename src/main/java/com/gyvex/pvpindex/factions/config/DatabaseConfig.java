package com.gyvex.pvpindex.factions.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed wrapper around the Bukkit {@link FileConfiguration} for {@code database.yml}.
 *
 * <p>Instantiated in {@link com.gyvex.pvpindex.factions.Bootstrap} during
 * {@code onEnable()} and re-created on {@code /fa reload}.
 */
public final class DatabaseConfig {

    private final FileConfiguration cfg;

    public DatabaseConfig(final FileConfiguration cfg) {
        this.cfg = cfg;
    }

    // -------------------------------------------------------------------------
    // General
    // -------------------------------------------------------------------------

    /** Returns the configured backend type: {@code "h2"} (default) or {@code "mysql"}. */
    public String getType() {
        return cfg.getString("type", "h2");
    }

    // -------------------------------------------------------------------------
    // H2
    // -------------------------------------------------------------------------

    /** Returns the H2 file path relative to the plugin data folder. */
    public String getH2File() {
        return cfg.getString("h2.file", "data/factions");
    }

    // -------------------------------------------------------------------------
    // MySQL / MariaDB
    // -------------------------------------------------------------------------

    public String getMysqlHost() {
        return cfg.getString("mysql.host", "localhost");
    }

    public int getMysqlPort() {
        return cfg.getInt("mysql.port", 3306);
    }

    public String getMysqlDatabase() {
        return cfg.getString("mysql.database", "factions");
    }

    public String getMysqlUsername() {
        return cfg.getString("mysql.username", "root");
    }

    public String getMysqlPassword() {
        return cfg.getString("mysql.password", "");
    }

    public int getMysqlPoolSize() {
        return cfg.getInt("mysql.pool-size", 10);
    }

    // -------------------------------------------------------------------------
    // Debug
    // -------------------------------------------------------------------------

    /** Returns {@code true} if Jaloquent SQL query logging should be enabled. */
    public boolean isJaloquentLoggingEnabled() {
        return cfg.getBoolean("debug.jaloquent-logging", false);
    }
}
