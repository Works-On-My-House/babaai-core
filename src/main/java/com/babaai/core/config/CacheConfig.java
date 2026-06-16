package com.babaai.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * L1 (Caffeine) cache infrastructure for babaai-core. PERF-1.2.
 *
 * <p>Builds a {@link CaffeineCacheManager} where each cache named under
 * {@code babaai.cache.specs} gets its own size/TTL, and any other cache name falls back to
 * {@code babaai.cache.default-spec}. All caches record stats so Micrometer/Actuator can
 * expose hit ratios (Epic 6/7). Setting {@code babaai.cache.enabled=false} swaps in a
 * {@link NoOpCacheManager}, turning every {@code @Cacheable} into a pass-through.
 *
 * <p><b>L2 / Redis — introduce later only if needed (Epic 1, "optional future").</b> Caffeine is
 * per-instance, so when core runs as MULTIPLE replicas each instance's cache diverges. At that
 * point swap this single Caffeine manager for a two-tier manager (Caffeine L1 near-cache in front
 * of a shared Redis L2) — e.g. a {@code CompositeCacheManager(caffeine, redis)} or a dedicated
 * near-cache impl, plus {@code spring-boot-starter-data-redis}. Until core is horizontally scaled,
 * L1-only is the correct, cheaper choice. NOTE: instant permission revocation (task 869dqmbfp) does
 * NOT use Redis — the gateway can't read core's in-process Caffeine, so core pushes pv bumps to the
 * gateway via a webhook and the gateway keeps its own Caffeine denylist. So a shared Redis is only
 * warranted here once core is horizontally scaled and needs an L2.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(AppProperties properties) {
        AppProperties.Cache cacheProperties = properties.getCache();
        if (!cacheProperties.isEnabled()) {
            return new NoOpCacheManager();
        }
        // L1 (Caffeine, in-process). See class Javadoc for where Redis L2 slots in later.
        CaffeineCacheManager manager = new CaffeineCacheManager();
        // Default builder for any cache name not explicitly specced.
        manager.setCaffeine(builder(cacheProperties.getDefaultSpec()));
        // Per-cache overrides (size + TTL) registered up front.
        cacheProperties.getSpecs().forEach(
                (name, spec) -> manager.registerCustomCache(name, builder(spec).build()));
        return manager;
    }

    private static Caffeine<Object, Object> builder(AppProperties.CacheSpec spec) {
        return Caffeine.newBuilder()
                .maximumSize(spec.getMaximumSize())
                .expireAfterWrite(spec.getTtl())
                .recordStats();
    }
}
