package com.babaai.core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Raw bytes of an uploaded import, kept apart from {@link RecipeImport} so metadata/list reads never
 * pull the blob. Maps {@code content} to Postgres {@code bytea} (no {@code @Lob}, which would select
 * the large-object/OID path). Behind {@link com.babaai.core.service.ImportFileStore}.
 */
@Entity
@Table(name = "recipe_import_files")
public class RecipeImportFile extends BaseEntity {

    @Column(name = "import_id", nullable = false, unique = true)
    private UUID importId;

    @Column(nullable = false)
    private byte[] content;

    public UUID getImportId() {
        return importId;
    }

    public void setImportId(UUID importId) {
        this.importId = importId;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
