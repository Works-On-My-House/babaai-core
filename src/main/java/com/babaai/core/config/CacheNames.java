package com.babaai.core.config;

/**
 * Canonical cache names for the L1 (Caffeine) cache layer.
 *
 * <p>Each constant must have a matching spec under {@code babaai.cache.specs.*} in
 * {@code application.yml}; any cache referenced without a spec falls back to
 * {@code babaai.cache.default-spec}. Reference these from {@code @Cacheable} annotations
 * so cache keys stay consistent across the codebase (see PERF-1.7 taxonomy).
 */
public final class CacheNames {

    /** Global recipe catalog — hot read path ({@code findByVerifiedTrueOrderByNameAsc}). PERF-1.3. */
    public static final String RECIPE_CATALOG = "recipeCatalog";

    /** Recipe category list — semi-static reference data. PERF-1.3. */
    public static final String RECIPE_CATEGORIES = "recipeCategories";

    /** Parsed JSON config + public config snapshots. PERF-1.4. */
    public static final String APP_CONFIG = "appConfig";

    private CacheNames() {
    }
}
