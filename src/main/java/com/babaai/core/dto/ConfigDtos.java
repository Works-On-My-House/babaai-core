package com.babaai.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class ConfigDtos {

    private ConfigDtos() {
    }

    public record PublicConfigResponse(
            @JsonProperty("ingredient_categories") List<String> ingredientCategories,
            @JsonProperty("ingredient_units") List<String> ingredientUnits,
            @JsonProperty("default_min_match_percent") double defaultMinMatchPercent,
            @JsonProperty("default_suggestion_limit") int defaultSuggestionLimit,
            @JsonProperty("default_page_size") int defaultPageSize,
            @JsonProperty("default_ingredient_category") String defaultIngredientCategory
    ) {
    }

    public record InferCategoryResponse(String name, String category) {
    }
}
