package com.pvpindex.factions.command;

/**
 * Database backend types available for parameterised command tests.
 *
 * <p>{@link #MOCK} is always present. {@link #H2} is always present.
 * {@link #MYSQL} is included only when {@code MYSQL_TEST_HOST} is set in the
 * environment.
 */
public enum StorageType {

    /** Mockito mock {@code Repositories} — no real I/O, fast unit-test baseline. */
    MOCK("Mock"),

    /** Embedded H2 database in MySQL-compat mode — always available, catches constraint bugs. */
    H2("H2"),

    /** External MySQL / MariaDB — active only when {@code MYSQL_TEST_HOST} is set. */
    MYSQL("MySQL");

    private final String displayName;

    StorageType(final String displayName) {
        this.displayName = displayName;
    }

    /** Short name used in JUnit test-result display, e.g. {@code "[H2]"}. */
    public String displayName() {
        return displayName;
    }
}
