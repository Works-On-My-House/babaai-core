package com.babaai.core.service;

import java.util.Optional;
import java.util.UUID;

/**
 * Storage port for uploaded recipe-import file bytes (ClickUp 869dtx7vk). The MVP backend is a
 * Postgres {@code bytea} table ({@link DbImportFileStore}); this interface lets us swap to object
 * storage (S3 / MinIO) later without touching the import service or controllers.
 */
public interface ImportFileStore {

    /** Persist (or replace) the bytes for an import. */
    void store(UUID importId, byte[] content);

    /** Load the raw bytes for an import, if present. */
    Optional<byte[]> load(UUID importId);
}
