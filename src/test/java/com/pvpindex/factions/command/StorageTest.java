package com.pvpindex.factions.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Drop-in replacement for {@code @Test} in command tests that runs each test
 * method once per available storage backend.
 *
 * <p>Backends are provided by {@link StorageTestExtension}:
 * <ul>
 *   <li><b>[Mock]</b> — Mockito mock {@code Repositories}; fast unit baseline.</li>
 *   <li><b>[H2]</b> — Embedded H2 with real schema; catches constraint bugs.</li>
 *   <li><b>[MySQL]</b> — External MySQL when {@code MYSQL_TEST_HOST} is set.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * @StorageTest
 * @DisplayName("success — faction created")
 * void testCreateSuccess() { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(StorageTestExtension.class)
public @interface StorageTest {
}
