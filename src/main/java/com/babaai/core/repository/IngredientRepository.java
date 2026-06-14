package com.babaai.core.repository;

import com.babaai.core.domain.Ingredient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID>, JpaSpecificationExecutor<Ingredient> {

    Optional<Ingredient> findByIdAndUserId(UUID id, UUID userId);

    List<Ingredient> findByUserIdAndIdIn(UUID userId, List<UUID> ids);
}
