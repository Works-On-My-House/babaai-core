package com.babaai.core.repository;

import com.babaai.core.domain.DailySuggestion;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailySuggestionRepository extends JpaRepository<DailySuggestion, UUID> {

    List<DailySuggestion> findByUserIdAndGeneratedOnOrderByRankAsc(UUID userId, LocalDate generatedOn);

    boolean existsByUserIdAndGeneratedOn(UUID userId, LocalDate generatedOn);

    @Modifying
    @Query("delete from DailySuggestion d where d.userId = :userId and d.generatedOn = :generatedOn")
    void deleteByUserIdAndGeneratedOn(@Param("userId") UUID userId, @Param("generatedOn") LocalDate generatedOn);

    // Recency signal: which recipes were already suggested in the trailing window (down-rank repeats).
    @Query("select distinct d.recipeId from DailySuggestion d where d.userId = :userId and d.generatedOn >= :since")
    List<UUID> findRecipeIdsSuggestedSince(@Param("userId") UUID userId, @Param("since") LocalDate since);

    @Modifying
    @Query("delete from DailySuggestion d where d.generatedOn < :cutoff")
    int deleteByGeneratedOnBefore(@Param("cutoff") LocalDate cutoff);
}
