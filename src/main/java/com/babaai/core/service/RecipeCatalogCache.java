package com.babaai.core.service;

import com.babaai.core.config.CacheNames;
import com.babaai.core.domain.Recipe;
import com.babaai.core.repository.RecipeRepository;
import java.util.List;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Caches the full recipe catalog (PERF-1.3) as an immutable, read-only snapshot.
 *
 * <p>The hot read paths (featured / dailyPicks / suggestions) each load the entire catalog with
 * ingredients and match in-memory; this keeps a single shared copy resident instead of re-querying
 * on every call. The snapshot must NEVER be mutated — per-user fields (favorite flags) are applied
 * at DTO-mapping time, not on the entities. View-count/popularity staleness is bounded by the
 * cache TTL (accepted trade-off). Catalog writes evict via {@link #evictAfterCommit()}.
 *
 * <p>Lives in its own bean so the {@code @Cacheable} proxy intercepts calls from
 * {@link RecipeService} (Spring AOP does not advise self-invocations).
 */
@Component
public class RecipeCatalogCache {

    private final RecipeRepository recipeRepository;
    private final CacheManager cacheManager;

    public RecipeCatalogCache(RecipeRepository recipeRepository, CacheManager cacheManager) {
        this.recipeRepository = recipeRepository;
        this.cacheManager = cacheManager;
    }

    @Cacheable(CacheNames.RECIPE_CATALOG)
    @Transactional(readOnly = true)
    public List<Recipe> snapshot() {
        // The repo method's @EntityGraph eagerly loads ingredients, so these detached entities are
        // safe to read after the session closes. Wrapped immutable to block structural mutation.
        return List.copyOf(recipeRepository.findAllByOrderByNameAsc());
    }

    /**
     * Evicts the cached catalog once the current transaction commits (or immediately when there is
     * no active transaction). Deferring to after-commit prevents a concurrent read from
     * repopulating the cache with pre-commit data and leaving it stale.
     */
    public void evictAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    clear();
                }
            });
        } else {
            clear();
        }
    }

    private void clear() {
        Cache cache = cacheManager.getCache(CacheNames.RECIPE_CATALOG);
        if (cache != null) {
            cache.clear();
        }
    }
}
