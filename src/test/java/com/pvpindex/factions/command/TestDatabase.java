package com.pvpindex.factions.command;

import com.pvpindex.factions.config.DatabaseConfig;
import com.pvpindex.factions.data.DatabaseManager;
import com.pvpindex.factions.data.Repositories;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.mockito.Mockito;

/**
 * Factory and lifecycle owner for test database instances.
 *
 * <p>Each factory method returns a self-contained fixture that owns its
 * {@link DatabaseManager} (and temp directory when applicable) and must be
 * {@linkplain #close() closed} after the test to release resources.
 *
 * <ul>
 *   <li>{@link #mock()} — pure Mockito mock, no I/O.</li>
 *   <li>{@link #h2()} — fresh embedded H2 in a temp dir, always available.</li>
 *   <li>{@link #newMysql()} — MySQL connection; only call when
 *       {@link #isMysqlConfigured()} returns {@code true}.</li>
 * </ul>
 */
public final class TestDatabase implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger("TestDatabase");

    private final StorageType storageType;
    private final Repositories repositories;
    private final DatabaseManager dbManager;
    private final Path tempDir;

    private TestDatabase(
            final StorageType storageType,
            final Repositories repositories,
            final DatabaseManager dbManager,
            final Path tempDir) {
        this.storageType = storageType;
        this.repositories = repositories;
        this.dbManager = dbManager;
        this.tempDir = tempDir;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link Repositories} for this test database.
     *
     * <p>For {@link StorageType#MOCK} this is a plain Mockito mock. For real
     * backends ({@link StorageType#H2}, {@link StorageType#MYSQL}) this is a
     * {@code Mockito.spy()} wrapping a real {@link Repositories} instance, so
     * existing stub declarations still work while un-stubbed calls reach the
     * actual database and can surface constraint violations.
     */
    public Repositories repositories() {
        return repositories;
    }

    /** Returns the storage type backing this fixture. */
    public StorageType storageType() {
        return storageType;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void close() {
        if (dbManager != null) {
            dbManager.close();
        }
        if (tempDir != null) {
            deleteDir(tempDir);
        }
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /** Creates a Mockito mock {@link Repositories}. No real I/O. */
    public static TestDatabase mock() {
        return new TestDatabase(StorageType.MOCK, Mockito.mock(Repositories.class), null, null);
    }

    /**
     * Creates a fresh embedded H2 database in a unique temporary directory,
     * sets up the full schema via {@link DatabaseManager}, and returns a
     * Mockito spy wrapping the real {@link Repositories}.
     *
     * @throws Exception if the database cannot be initialised
     */
    public static TestDatabase h2() throws Exception {
        final Path dir = Files.createTempDirectory("pvpindex-test-h2-");
        final DatabaseConfig cfg = h2Config(dir.toFile());
        final DatabaseManager mgr = new DatabaseManager();
        mgr.initialize(cfg, dir.toFile(), LOG);
        final Repositories repos = Mockito.spy(new Repositories(mgr.getStore()));
        return new TestDatabase(StorageType.H2, repos, mgr, dir);
    }

    /**
     * Returns {@code true} when the {@code MYSQL_TEST_HOST} environment variable
     * is set and non-blank, indicating that a MySQL backend is available for
     * integration tests.
     */
    public static boolean isMysqlConfigured() {
        final String host = System.getenv("MYSQL_TEST_HOST");
        return host != null && !host.isBlank();
    }

    /**
     * Creates a MySQL-backed test database using connection details from
     * environment variables:
     * <ul>
     *   <li>{@code MYSQL_TEST_HOST} (required)</li>
     *   <li>{@code MYSQL_TEST_PORT} (default {@code 3306})</li>
     *   <li>{@code MYSQL_TEST_DB} (default {@code pvpindex_test})</li>
     *   <li>{@code MYSQL_TEST_USER} (default {@code root})</li>
     *   <li>{@code MYSQL_TEST_PASS} (default {@code ""})</li>
     * </ul>
     *
     * <p>Only call this when {@link #isMysqlConfigured()} returns {@code true}.
     *
     * @throws Exception if the database cannot be initialised
     */
    public static TestDatabase newMysql() throws Exception {
        final String host = System.getenv("MYSQL_TEST_HOST");
        final int port = intEnv("MYSQL_TEST_PORT", 3306);
        final String db = strEnv("MYSQL_TEST_DB", "pvpindex_test");
        final String user = strEnv("MYSQL_TEST_USER", "root");
        final String pass = strEnv("MYSQL_TEST_PASS", "");

        final YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("type", "mysql");
        yaml.set("mysql.host", host);
        yaml.set("mysql.port", port);
        yaml.set("mysql.database", db);
        yaml.set("mysql.username", user);
        yaml.set("mysql.password", pass);
        yaml.set("mysql.pool-size", 2);
        yaml.set("debug.jaloquent-logging", false);

        final DatabaseConfig cfg = new DatabaseConfig(yaml);
        final Path dir = Files.createTempDirectory("pvpindex-test-mysql-");
        final DatabaseManager mgr = new DatabaseManager();
        mgr.initialize(cfg, dir.toFile(), LOG);
        final Repositories repos = Mockito.spy(new Repositories(mgr.getStore()));
        return new TestDatabase(StorageType.MYSQL, repos, mgr, dir);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static DatabaseConfig h2Config(final File dataDir) {
        final YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("type", "h2");
        yaml.set("h2.file", new File(dataDir, "factions").getAbsolutePath());
        yaml.set("debug.jaloquent-logging", false);
        return new DatabaseConfig(yaml);
    }

    private static String strEnv(final String key, final String fallback) {
        final String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : fallback;
    }

    private static int intEnv(final String key, final int fallback) {
        final String val = System.getenv(key);
        if (val == null || val.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void deleteDir(final Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException ignored) {
            // Best-effort cleanup; temp files are removed by the OS on JVM exit.
        }
    }
}
