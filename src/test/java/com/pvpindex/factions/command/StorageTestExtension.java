package com.pvpindex.factions.command;

import com.pvpindex.factions.data.Repositories;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * JUnit 5 extension that drives {@link StorageTest @StorageTest}.
 *
 * <p>For each test method annotated with {@code @StorageTest} this provider
 * yields one invocation context per available storage backend:
 * <ol>
 *   <li><b>[Mock]</b> — always present; {@code Repositories} is a plain
 *       Mockito mock (current unit-test baseline).</li>
 *   <li><b>[H2]</b> — always present; {@code Repositories} is a Mockito spy
 *       wrapping a real embedded H2 database with the full schema. Un-stubbed
 *       repository calls reach the actual database and surface constraint
 *       violations.</li>
 *   <li><b>[MySQL]</b> — present only when {@code MYSQL_TEST_HOST} is set in
 *       the environment; same spy pattern as H2.</li>
 * </ol>
 *
 * <p>Before each invocation the extension injects the appropriate
 * {@link Repositories} instance into the {@code protected Repositories repos}
 * field declared in {@link CommandTestBase} (or any superclass).  The original
 * {@code @Mock}-initialised value is silently replaced; tests using
 * {@code @Test} are unaffected.
 */
public final class StorageTestExtension implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(final ExtensionContext context) {
        return true; // Gated by @ExtendWith on @StorageTest itself.
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            final ExtensionContext context) {

        final List<TestTemplateInvocationContext> ctxs = new ArrayList<>();

        // Always: plain Mockito mock (fast unit-test baseline).
        ctxs.add(invocationContext(StorageType.MOCK, TestDatabase::mock));

        // Always: embedded H2 — fresh database per test invocation.
        ctxs.add(invocationContext(StorageType.H2, TestDatabase::h2));

        // Conditional: MySQL only when env var is configured.
        if (TestDatabase.isMysqlConfigured()) {
            ctxs.add(invocationContext(StorageType.MYSQL, TestDatabase::newMysql));
        }

        return ctxs.stream();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static TestTemplateInvocationContext invocationContext(
            final StorageType type, final DatabaseSupplier supplier) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(final int invocationIndex) {
                return "[" + type.displayName() + "]";
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(new ReposInjector(supplier));
            }
        };
    }

    // -------------------------------------------------------------------------
    // ReposInjector — BeforeEach creates DB, AfterEach closes it
    // -------------------------------------------------------------------------

    /**
     * Per-invocation extension that creates a {@link TestDatabase} in
     * {@code beforeEach}, injects its {@link Repositories} into the test
     * instance, and closes the database in {@code afterEach}.
     */
    static final class ReposInjector implements BeforeEachCallback, AfterEachCallback {

        private final DatabaseSupplier supplier;
        private TestDatabase db;

        ReposInjector(final DatabaseSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public void beforeEach(final ExtensionContext context) throws Exception {
            db = supplier.get();
            injectField("repos", context.getRequiredTestInstance(), db.repositories());
        }

        @Override
        public void afterEach(final ExtensionContext context) {
            if (db != null) {
                db.close();
                db = null;
            }
        }

        /**
         * Walks the test-instance class hierarchy to find a field named
         * {@code fieldName} and sets it to {@code value}.
         */
        private static void injectField(
                final String fieldName, final Object instance, final Object value) {
            for (Class<?> c = instance.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    final Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(instance, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    // Walk up to the next superclass.
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Cannot inject field '" + fieldName + "'", e);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Functional interface for database creation
    // -------------------------------------------------------------------------

    /** Supplier that may throw a checked exception during database setup. */
    @FunctionalInterface
    interface DatabaseSupplier {
        TestDatabase get() throws Exception;
    }
}
