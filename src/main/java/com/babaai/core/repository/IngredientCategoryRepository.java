package com.babaai.core.repository;

import com.babaai.core.domain.IngredientCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientCategoryRepository extends JpaRepository<IngredientCategory, UUID> {

    Optional<IngredientCategory> findByName(String name);

    Optional<IngredientCategory> findFirstByIsDefaultTrue();

    List<IngredientCategory> findAllByOrderByNameAsc();
}
