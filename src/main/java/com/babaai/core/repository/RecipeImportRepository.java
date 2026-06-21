package com.babaai.core.repository;

import com.babaai.core.domain.RecipeImport;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeImportRepository extends JpaRepository<RecipeImport, UUID> {

    List<RecipeImport> findBySubmittedByOrderByCreatedAtDesc(UUID submittedBy);

    // Used by the admin moderation slice (queue of pending submissions).
    List<RecipeImport> findByStatusOrderByCreatedAtAsc(String status);
}
