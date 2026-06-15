package com.babaai.core;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Single PostgreSQL container shared across all integration test classes in one JVM.
 * Manual lifecycle avoids Testcontainers/JUnit re-initialization issues between test classes.
 */
public final class PostgresTestContainer {

    private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("babaai")
            .withUsername("babaai")
            .withPassword("babaai");

    private PostgresTestContainer() {
    }

    public static PostgreSQLContainer<?> get() {
        if (!INSTANCE.isRunning()) {
            INSTANCE.start();
        }
        return INSTANCE;
    }
}
