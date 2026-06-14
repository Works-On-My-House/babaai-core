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

    @EntityGraph(attributePaths = "ingredients")
    List<Recipe> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("select distinct r.category from Recipe r order by r.category asc")
    List<String> findDistinctCategories();
}
