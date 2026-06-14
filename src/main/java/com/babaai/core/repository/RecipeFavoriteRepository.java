package com.babaai.core.repository;

import com.babaai.core.domain.RecipeFavorite;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecipeFavoriteRepository extends JpaRepository<RecipeFavorite, UUID> {

    Optional<RecipeFavorite> findByUserIdAndRecipeId(UUID userId, UUID recipeId);

    @Query("select rf.recipeId from RecipeFavorite rf where rf.userId = :userId")
    Set<UUID> findRecipeIdsByUserId(UUID userId);

    @Query("select rf.recipeId, count(rf.id) from RecipeFavorite rf group by rf.recipeId")
    List<Object[]> countGroupedByRecipeId();

    @Query("select rf.recipeId, count(rf.id) from RecipeFavorite rf where rf.recipeId in :recipeIds group by rf.recipeId")
    List<Object[]> countGroupedByRecipeIdIn(List<UUID> recipeIds);

    void deleteByUserIdAndRecipeId(UUID userId, UUID recipeId);
}
