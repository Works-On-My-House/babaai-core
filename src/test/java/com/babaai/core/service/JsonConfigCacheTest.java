package com.babaai.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.babaai.core.config.AppProperties;
import com.babaai.core.config.CacheConfig;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.json.JsonMapper;

@SpringJUnitConfig(JsonConfigCacheTest.TestConfig.class)
class JsonConfigCacheTest {

    @Autowired
    private JsonConfigCache jsonConfigCache;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    void parsesResourceOnceThenServesFromCache() {
        Map<String, Object> first = jsonConfigCache.loadObject("classpath:cache-test.json");
        Map<String, Object> second = jsonConfigCache.loadObject("classpath:cache-test.json");

        assertThat(first).containsEntry("greeting", "hi");
        // @Cacheable returns the same stored instance, and the resource is loaded only once.
        assertThat(second).isSameAs(first);
        verify(resourceLoader, times(1)).getResource("classpath:cache-test.json");
    }

    @Configuration
    @Import(CacheConfig.class)
    static class TestConfig {

        @Bean
        AppProperties appProperties() {
            return new AppProperties();
        }

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        ResourceLoader resourceLoader() throws Exception {
            ResourceLoader loader = mock(ResourceLoader.class);
            Resource resource = mock(Resource.class);
            when(loader.getResource("classpath:cache-test.json")).thenReturn(resource);
            when(resource.getInputStream()).thenAnswer(invocation ->
                    new ByteArrayInputStream("{\"greeting\":\"hi\"}".getBytes(StandardCharsets.UTF_8)));
            return loader;
        }

        @Bean
        JsonConfigCache jsonConfigCache(ResourceLoader resourceLoader, JsonMapper jsonMapper) {
            return new JsonConfigCache(resourceLoader, jsonMapper);
        }
    }
}
