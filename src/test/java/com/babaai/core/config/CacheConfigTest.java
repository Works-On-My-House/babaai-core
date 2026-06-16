package com.babaai.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;

class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void registersNamedCacheFromSpecAndStoresValues() {
        AppProperties properties = new AppProperties();
        AppProperties.CacheSpec spec = new AppProperties.CacheSpec();
        spec.setMaximumSize(10);
        spec.setTtl(Duration.ofMinutes(5));
        properties.getCache().getSpecs().put(CacheNames.RECIPE_CATALOG, spec);

        CacheManager manager = cacheConfig.cacheManager(properties);

        assertThat(manager).isInstanceOf(CaffeineCacheManager.class);
        Cache cache = manager.getCache(CacheNames.RECIPE_CATALOG);
        assertThat(cache).isNotNull();

        cache.put("k", "v");
        assertThat(cache.get("k", String.class)).isEqualTo("v");
    }

    @Test
    void createsUnlistedCacheFromDefaultSpec() {
        AppProperties properties = new AppProperties();

        CacheManager manager = cacheConfig.cacheManager(properties);

        // No spec registered for this name -> created on demand via the default Caffeine builder.
        assertThat(manager.getCache("someOtherCache")).isNotNull();
    }

    @Test
    void disabledCacheUsesNoOpManager() {
        AppProperties properties = new AppProperties();
        properties.getCache().setEnabled(false);

        assertThat(cacheConfig.cacheManager(properties)).isInstanceOf(NoOpCacheManager.class);
    }
}
