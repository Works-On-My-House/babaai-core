package com.babaai.core.service;

import com.babaai.core.config.CacheNames;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Caches the file IO + JSON parse for config resources. PERF-1.4.
 *
 * <p>Lives in its own bean (not inline in {@link JsonConfigService}) so the {@code @Cacheable}
 * proxy actually intercepts the call — Spring AOP does not advise self-invocations. Config files
 * are classpath resources that don't change at runtime, so each location is parsed once and the
 * raw map is reused. The typed accessors in {@link JsonConfigService} build fresh collections from
 * this map on every call, so callers never mutate the cached value; treat the returned map as
 * read-only regardless.
 */
@Component
public class JsonConfigCache {

    private final ResourceLoader resourceLoader;
    private final JsonMapper jsonMapper;

    public JsonConfigCache(ResourceLoader resourceLoader, JsonMapper jsonMapper) {
        this.resourceLoader = resourceLoader;
        this.jsonMapper = jsonMapper;
    }

    @Cacheable(CacheNames.APP_CONFIG)
    @SuppressWarnings("unchecked")
    public Map<String, Object> loadObject(String location) {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream inputStream = resource.getInputStream()) {
            return jsonMapper.readValue(inputStream, Map.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load config: " + location, ex);
        }
    }
}
