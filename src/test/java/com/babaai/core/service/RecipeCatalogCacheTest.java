package com.babaai.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.babaai.core.config.AppProperties;
import com.babaai.core.config.CacheConfig;
import com.babaai.core.domain.Recipe;
import com.babaai.core.repository.RecipeRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(RecipeCatalogCacheTest.TestConfig.class)
class RecipeCatalogCacheTest {

    @Autowired
    private RecipeCatalogCache catalogCache;

    @Autowired
    private RecipeRepository recipeRepository;

    @Test
    void loadsCatalogOnceThenServesFromCacheUntilEvicted() {
        Recipe recipe = new Recipe();
        recipe.setName("Soup");
        recipe.setVerified(true);
        when(recipeRepository.findByVerifiedTrueOrderByNameAsc()).thenReturn(List.of(recipe));

        List<Recipe> first = catalogCache.snapshot();
        List<Recipe> second = catalogCache.snapshot();

        assertThat(second).isSameAs(first);
        verify(recipeRepository, times(1)).findByVerifiedTrueOrderByNameAsc();

        // No active transaction in this test -> evictAfterCommit clears immediately.
        catalogCache.evictAfterCommit();
        catalogCache.snapshot();

        verify(recipeRepository, times(2)).findByVerifiedTrueOrderByNameAsc();
    }

    @Configuration
    @Import(CacheConfig.class)
    static class TestConfig {

        @Bean
        AppProperties appProperties() {
            return new AppProperties();
        }

        @Bean
        RecipeRepository recipeRepository() {
            return mock(RecipeRepository.class);
        }

        @Bean
        RecipeCatalogCache recipeCatalogCache(RecipeRepository recipeRepository, CacheManager cacheManager) {
            return new RecipeCatalogCache(recipeRepository, cacheManager);
        }
    }
}
