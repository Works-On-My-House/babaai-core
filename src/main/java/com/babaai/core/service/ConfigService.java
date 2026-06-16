package com.babaai.core.service;

import com.babaai.core.config.AppProperties;
import com.babaai.core.config.CacheNames;
import com.babaai.core.dto.ConfigDtos;
import com.babaai.core.repository.IngredientCategoryRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigService {

    private final IngredientCategoryRepository categoryRepository;
    private final JsonConfigService jsonConfigService;
    private final AppProperties appProperties;

    public ConfigService(
            IngredientCategoryRepository categoryRepository,
            JsonConfigService jsonConfigService,
            AppProperties appProperties
    ) {
        this.categoryRepository = categoryRepository;
        this.jsonConfigService = jsonConfigService;
        this.appProperties = appProperties;
    }

    // PERF-1.4: immutable record, semi-static category data — safe to cache. TTL handles staleness;
    // when category CRUD is added, evict CacheNames.APP_CONFIG on category writes.
    @Cacheable(CacheNames.APP_CONFIG)
    @Transactional(readOnly = true)
    public ConfigDtos.PublicConfigResponse publicConfig() {
        return new ConfigDtos.PublicConfigResponse(
                categoryRepository.findAllByOrderByNameAsc().stream().map(category -> category.getName()).toList(),
                jsonConfigService.ingredientUnits(),
                appProperties.getDefaults().getMinMatchPercent(),
                appProperties.getDefaults().getSuggestionLimit(),
                appProperties.getDefaults().getPageSize(),
                categoryRepository.findFirstByIsDefaultTrue()
                        .map(category -> category.getName())
                        .orElse(appProperties.getDefaults().getIngredientCategory())
        );
    }
}
