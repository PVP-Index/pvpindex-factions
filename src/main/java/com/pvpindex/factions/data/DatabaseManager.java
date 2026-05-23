package com.pvpindex.factions.data;

import com.github.ezframework.jaloquent.config.JaloquentConfig;
import com.github.ezframework.jaloquent.store.sql.DataSourceJdbcStore;
import com.pvpindex.factions.config.DatabaseConfig;
import java.util.List;
import java.util.Locale;
import javax.sql.DataSource;
import com.pvpindex.factions.data.model.AuditLogModel;
import com.pvpindex.factions.data.model.BankTransactionModel;
import com.pvpindex.factions.data.model.BoardEntry;
import com.pvpindex.factions.data.model.FactionInboxEntry;
import com.pvpindex.factions.data.model.FactionModel;
import com.pvpindex.factions.data.model.InvitationModel;
import com.pvpindex.factions.data.model.PlayerModel;
import com.pvpindex.factions.data.model.PowerHistoryModel;
import com.pvpindex.factions.data.model.RankModel;
import com.pvpindex.factions.data.model.WarpModel;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the HikariCP connection pool and Jaloquent {@link DataSourceJdbcStore}.
 *
 * <p>Supports two backends:
 * <ul>
 *   <li><b>H2</b> — embedded file-based database opened in MySQL-compatibility
 *   mode. Requires no external server; suitable for solo / small servers.
 *   H2 2.x does not implement the {@code VALUES(col)} function reference that
 *   Jaloquent uses inside {@code ON DUPLICATE KEY UPDATE}, so upsert SQL is
 *   transparently rewritten to {@code MERGE INTO … KEY(id)} by the internal
 *   {@code H2CompatJdbcStore} before execution.
 *   The H2 driver is shaded into the plugin JAR and loaded explicitly via
 *   {@code driverClassName} to avoid Bukkit classloader isolation issues.</li>
 *   <li><b>MySQL / MariaDB</b> — external server via JDBC. The mysql-connector-java
 *   (or mariadb-java-client) jar must be available on the server classpath.</li>
 * </ul>
 *
 * <p>Call {@link #initialize(DatabaseConfig, File, Logger)} in {@code onEnable()},
 * then {@link #close()} in {@code onDisable()}.
 */
public final class DatabaseManager {

    private HikariDataSource dataSource;
    private DataSourceJdbcStore store;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Open the connection pool and validate the schema.
     *
     * @param dbCfg   database configuration (from {@code database.yml})
     * @param dataDir plugin data folder (used for H2 file path)
     * @param logger  plugin logger
     * @throws IllegalStateException if the database cannot be opened
     */
    public void initialize(
            final DatabaseConfig dbCfg, final File dataDir, final Logger logger) {

        final HikariConfig hk = new HikariConfig();

        final boolean isH2 = !"mysql".equalsIgnoreCase(dbCfg.getType());

        if ("mysql".equalsIgnoreCase(dbCfg.getType())) {
            hk.setJdbcUrl("jdbc:mysql://" + dbCfg.getMysqlHost() + ":" + dbCfg.getMysqlPort()
                + "/" + dbCfg.getMysqlDatabase() + "?useSSL=false&allowPublicKeyRetrieval=true"
                + "&characterEncoding=UTF-8&serverTimezone=UTC");
            hk.setDriverClassName(detectMysqlDriverClass());
            hk.setUsername(dbCfg.getMysqlUsername());
            hk.setPassword(dbCfg.getMysqlPassword());
            hk.setMaximumPoolSize(dbCfg.getMysqlPoolSize());
            hk.setMinimumIdle(2);
        } else {
            // Default: H2 embedded in MySQL-compatibility mode.
            // The H2 driver is shaded and relocated; set driverClassName explicitly
            // so HikariCP loads it via Class.forName() instead of DriverManager.getDriver(),
            // which fails under Bukkit's classloader isolation.
            final File h2File = new File(dataDir, dbCfg.getH2File());
            h2File.getParentFile().mkdirs();
            hk.setJdbcUrl("jdbc:h2:file:" + h2File.getAbsolutePath()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE");
            hk.setDriverClassName(detectH2DriverClass());
            hk.setUsername("sa");
            hk.setPassword("");
            hk.setMaximumPoolSize(1); // H2 file DB is single-writer
            hk.setMinimumIdle(1);
        }

        hk.setPoolName("PvPIndexFactions-DB");
        hk.setConnectionTimeout(10_000);
        hk.setIdleTimeout(600_000);
        hk.setMaxLifetime(1_800_000);

        JaloquentConfig.enableLogging(dbCfg.isJaloquentLoggingEnabled());

        try {
            dataSource = new HikariDataSource(hk);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to open database connection pool", e);
        }

        store = isH2 ? new H2CompatJdbcStore(dataSource) : new DataSourceJdbcStore(dataSource);
        createTables(logger);
    }

    /** Close the connection pool. Safe to call even if not initialized. */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // -------------------------------------------------------------------------
    // Schema creation
    // -------------------------------------------------------------------------

    private void createTables(final Logger logger) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(createTableSql("factions", FactionModel.COLUMNS, "id"));
            stmt.executeUpdate(createTableSql("players", PlayerModel.COLUMNS, "id"));
            stmt.executeUpdate(createTableSql("board", BoardEntry.COLUMNS, "id"));
            stmt.executeUpdate(createTableSql("warps", WarpModel.COLUMNS, "id"));
            stmt.executeUpdate(createTableSql("invitations", InvitationModel.COLUMNS, "id"));
            stmt.executeUpdate(createTableSql("ranks", RankModel.COLUMNS, "id"));
            stmt.executeUpdate(createTableSql("bank_transactions", BankTransactionModel.COLUMNS, "id"));
            stmt.executeUpdate(createTableSql("power_history", PowerHistoryModel.COLUMNS, "id"));
            stmt.executeUpdate(createTableSql("faction_inbox", FactionInboxEntry.COLUMNS, "id"));
            stmt.executeUpdate(createTableSql("audit_logs", AuditLogModel.COLUMNS, "id"));
            ensureColumn(
                stmt,
                logger,
                "players",
                "auto_territory_mode",
                "TINYINT NOT NULL DEFAULT 0");
            ensureColumn(
                stmt,
                logger,
                "players",
                "notify_invites",
                "TINYINT NOT NULL DEFAULT 1");
            ensureColumn(
                stmt,
                logger,
                "players",
                "notify_bank_tax",
                "TINYINT NOT NULL DEFAULT 1");
            ensureColumn(
                stmt,
                logger,
                "players",
                "notify_motd",
                "TINYINT NOT NULL DEFAULT 1");
            ensureColumn(
                stmt,
                logger,
                "players",
                "locale",
                "VARCHAR(16)");
            ensureColumn(
                stmt,
                logger,
                "players",
                "power_frozen",
                "TINYINT NOT NULL DEFAULT 0");
            // 1.1.3 — warp passwords and per-use costs
            ensureColumn(
                stmt,
                logger,
                "warps",
                "password",
                "VARCHAR(64)");
            ensureColumn(
                stmt,
                logger,
                "warps",
                "use_cost",
                "DOUBLE NOT NULL DEFAULT 0.0");
            createIndexes(stmt, logger);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to create database tables", e);
            throw new IllegalStateException("Schema creation failed", e);
        }
    }

    private void createIndexes(final Statement stmt, final Logger logger) {
        // Faction lookups and uniqueness by name.
        createIndex(stmt, logger, "idx_factions_name", "factions", "name");
        // Member and rank lookups within a faction.
        createIndex(stmt, logger, "idx_players_faction_id", "players", "faction_id");
        createIndex(stmt, logger, "idx_ranks_faction_id", "ranks", "faction_id");
        // Claim scans per faction (land counts/lists).
        createIndex(stmt, logger, "idx_board_faction_id", "board", "faction_id");
        // Warp/invite scans by owning faction and invitee checks.
        createIndex(stmt, logger, "idx_warps_faction_id", "warps", "faction_id");
        createIndex(stmt, logger, "idx_invitations_faction_id", "invitations", "faction_id");
        createIndex(stmt, logger, "idx_invitations_invitee_id", "invitations", "invitee_id");
        createIndex(
            stmt, logger, "idx_invitations_faction_invitee", "invitations", "faction_id, invitee_id");
        // Bank history and faction-scoped transaction scans.
        createIndex(stmt, logger, "idx_bank_tx_faction_id", "bank_transactions", "faction_id");
        createIndex(
            stmt, logger, "idx_bank_tx_faction_created_at", "bank_transactions", "faction_id, created_at");
        // Power history scans by player and time.
        createIndex(stmt, logger, "idx_power_history_player_uuid", "power_history", "player_uuid");
        createIndex(
            stmt, logger, "idx_power_history_player_created_at", "power_history", "player_uuid, created_at");
        // Inbox entries per player.
        createIndex(stmt, logger, "idx_inbox_player_id", "faction_inbox", "player_id");
        // Audit log scans per faction, ordered by time.
        createIndex(stmt, logger, "idx_audit_logs_faction_id", "audit_logs", "faction_id");
        createIndex(
            stmt, logger, "idx_audit_logs_faction_created_at", "audit_logs", "faction_id, created_at");
    }

    private void createIndex(
            final Statement stmt,
            final Logger logger,
            final String indexName,
            final String tableName,
            final String columnList) {
        try {
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS `" + indexName + "` ON `" + tableName + "` (" + columnList + ")");
        } catch (SQLException ignored) {
            // Some engines/versions may not support IF NOT EXISTS for indexes;
            // retry without it and ignore duplicate-index style failures.
            try {
                stmt.executeUpdate(
                    "CREATE INDEX `" + indexName + "` ON `" + tableName + "` (" + columnList + ")");
            } catch (SQLException e) {
                final String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                if (!msg.contains("exists") && !msg.contains("duplicate") && !msg.contains("already")) {
                    logger.log(Level.FINE, "Index creation skipped for " + indexName + ": " + e.getMessage());
                }
            }
        }
    }

    private void ensureColumn(
            final Statement stmt,
            final Logger logger,
            final String tableName,
            final String columnName,
            final String columnSql) {
        try {
            stmt.executeUpdate(
                "ALTER TABLE `" + tableName + "` ADD COLUMN IF NOT EXISTS `"
                    + columnName + "` " + columnSql);
        } catch (SQLException ignored) {
            // Some engines/versions may not support IF NOT EXISTS for columns.
            try {
                stmt.executeUpdate(
                    "ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + columnSql);
            } catch (SQLException e) {
                final String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                if (!msg.contains("exists") && !msg.contains("duplicate") && !msg.contains("already")) {
                    logger.log(Level.FINE, "Column ensure skipped for " + tableName + "." + columnName + ": " + e.getMessage());
                }
            }
        }
    }

    private static String createTableSql(
            final String tableName,
            final java.util.Map<String, String> columns,
            final String primaryKey) {
        final StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS `");
        sb.append(tableName).append("` (");
        boolean first = true;
        for (final java.util.Map.Entry<String, String> entry : columns.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("`").append(entry.getKey()).append("` ").append(entry.getValue());
            first = false;
        }
        sb.append(", PRIMARY KEY (`").append(primaryKey).append("`)");
        sb.append(")");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Returns the Jaloquent {@link DataSourceJdbcStore} backed by this manager's pool. */
    public DataSourceJdbcStore getStore() {
        if (store == null) {
            throw new IllegalStateException("DatabaseManager is not initialized");
        }
        return store;
    }

    /** Returns {@code true} if the manager has been successfully initialized. */
    public boolean isInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }

    // -------------------------------------------------------------------------
    // Driver class detection
    // -------------------------------------------------------------------------

    /**
     * Returns the H2 driver class name, preferring the shaded variant used in
     * the packaged JAR ({@code com.pvpindex.lib.h2.Driver}) and falling
     * back to the unshaded one ({@code org.h2.Driver}) when running in tests.
     */
    static String detectH2DriverClass() {
        try {
            Class.forName("com.pvpindex.lib.h2.Driver");
            return "com.pvpindex.lib.h2.Driver";
        } catch (ClassNotFoundException ignored) {
            return "org.h2.Driver";
        }
    }

    /**
     * Returns the MySQL driver class name, preferring the shaded variant
     * ({@code com.pvpindex.lib.mysql.cj.jdbc.Driver}) and falling back
     * to the unshaded one for test contexts.
     */
    static String detectMysqlDriverClass() {
        try {
            Class.forName("com.pvpindex.lib.mysql.cj.jdbc.Driver");
            return "com.pvpindex.lib.mysql.cj.jdbc.Driver";
        } catch (ClassNotFoundException ignored) {
            return "com.mysql.cj.jdbc.Driver";
        }
    }

    // -------------------------------------------------------------------------
    // H2 upsert compatibility
    // -------------------------------------------------------------------------

    /**
     * Wraps {@link DataSourceJdbcStore} and rewrites MySQL-specific upsert SQL
     * into the H2-compatible {@code MERGE INTO} syntax before execution.
     *
     * <p>Jaloquent generates:
     * <pre>INSERT INTO `t` (`id`, `col`) VALUES (?) ON DUPLICATE KEY UPDATE `col`=VALUES(`col`)</pre>
     * H2 2.x does not implement the {@code VALUES(col)} function reference inside
     * {@code ON DUPLICATE KEY UPDATE}. This rewrite converts the statement to:
     * <pre>MERGE INTO `t` (`id`, `col`) KEY(`id`) VALUES (?)</pre>
     * which H2 fully supports in both embedded and MySQL-compat-mode configurations.
     */
    static final class H2CompatJdbcStore extends DataSourceJdbcStore {

        H2CompatJdbcStore(final DataSource dataSource) {
            super(dataSource);
        }

        @Override
        public int executeUpdate(final String sql, final List<Object> params) throws Exception {
            return super.executeUpdate(h2Upsert(sql), params);
        }

        /**
         * Rewrites a MySQL {@code ON DUPLICATE KEY UPDATE col=VALUES(col)} upsert
         * to an H2-compatible {@code MERGE INTO ... KEY(`id`) VALUES (...)} statement.
         *
         * @param sql original SQL string from Jaloquent
         * @return rewritten SQL, or the original if no rewrite is needed
         */
        static String h2Upsert(final String sql) {
            final int odku = sql.indexOf(" ON DUPLICATE KEY UPDATE ");
            if (odku < 0) {
                return sql;
            }
            final String beforeOdku = sql.substring(0, odku);
            final int valuesIdx = beforeOdku.toUpperCase(Locale.ROOT).lastIndexOf(" VALUES ");
            if (valuesIdx < 0) {
                return sql;
            }
            // "INSERT INTO `t` (cols) VALUES (?)" → "MERGE INTO `t` (cols) KEY(`id`) VALUES (?)"
            return "MERGE INTO"
                + beforeOdku.substring("INSERT INTO".length(), valuesIdx)
                + " KEY(`id`)"
                + beforeOdku.substring(valuesIdx);
        }
    }
}
