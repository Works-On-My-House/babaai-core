package com.babaai.core.repository;

import com.babaai.core.domain.RecipeImportFile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeImportFileRepository extends JpaRepository<RecipeImportFile, UUID> {

    Optional<RecipeImportFile> findByImportId(UUID importId);
}
