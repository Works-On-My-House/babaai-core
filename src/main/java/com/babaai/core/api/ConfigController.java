package com.babaai.core.api;

import com.babaai.core.dto.ConfigDtos;
import com.babaai.core.service.ConfigService;
import com.babaai.core.service.IngredientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final ConfigService configService;
    private final IngredientService ingredientService;

    public ConfigController(ConfigService configService, IngredientService ingredientService) {
        this.configService = configService;
        this.ingredientService = ingredientService;
    }

    @GetMapping("/public")
    public ConfigDtos.PublicConfigResponse publicConfig() {
        return configService.publicConfig();
    }

    @GetMapping("/infer-category")
    public ConfigDtos.InferCategoryResponse inferCategory(@RequestParam String name) {
        return ingredientService.inferCategory(name);
    }
}
