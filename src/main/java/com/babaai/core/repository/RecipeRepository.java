package com.babaai.core.repository;

import com.babaai.core.domain.Recipe;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface RecipeRepository extends JpaRepository<Recipe, UUID>, JpaSpecificationExecutor<Recipe> {

    @EntityGraph(attributePaths = "ingredients")
    Optional<Recipe> findWithIngredientsById(UUID id);

    // Verification gating (869dqrre0): the public detail path resolves only verified recipes;
    // unverified recipes 404.
    @EntityGraph(attributePaths = "ingredients")
    Optional<Recipe> findWithIngredientsByIdAndVerifiedTrue(UUID id);

    // Feeds the catalog cache -> featured / dailyPicks / suggestions / today all become verified-only.
    @EntityGraph(attributePaths = "ingredients")
    List<Recipe> findByVerifiedTrueOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("select distinct r.category from Recipe r where r.verified = true order by r.category asc")
    List<String> findDistinctVerifiedCategories();
}
