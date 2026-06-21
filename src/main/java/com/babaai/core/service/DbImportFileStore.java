package com.babaai.core.service;

import com.babaai.core.domain.RecipeImportFile;
import com.babaai.core.repository.RecipeImportFileRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Postgres {@code bytea} backend for {@link ImportFileStore} (MVP — no extra infra, fits the
 * Caffeine/no-Redis posture). Bytes live in {@code recipe_import_files}, separate from the queryable
 * {@code recipe_imports} metadata so listing never loads blobs.
 */
@Component
public class DbImportFileStore implements ImportFileStore {

    private final RecipeImportFileRepository repository;

    public DbImportFileStore(RecipeImportFileRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void store(UUID importId, byte[] content) {
        RecipeImportFile file = repository.findByImportId(importId).orElseGet(RecipeImportFile::new);
        file.setImportId(importId);
        file.setContent(content);
        repository.save(file);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> load(UUID importId) {
        return repository.findByImportId(importId).map(RecipeImportFile::getContent);
    }
}
