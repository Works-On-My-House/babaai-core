package com.babaai.core.service;

import com.babaai.core.config.AppProperties;
import com.babaai.core.dto.ConfigDtos;
import com.babaai.core.repository.IngredientCategoryRepository;
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
