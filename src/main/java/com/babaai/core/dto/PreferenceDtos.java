package com.babaai.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class PreferenceDtos {

    private PreferenceDtos() {
    }

    public record PreferencesResponse(
            @JsonProperty("preferred_ingredients") List<String> preferredIngredients,
            @JsonProperty("disliked_ingredients") List<String> dislikedIngredients,
            @JsonProperty("preferred_categories") List<String> preferredCategories,
            @JsonProperty("dietary_tags") List<String> dietaryTags,
            List<String> allergens
    ) {
    }

    public record PreferencesUpdateRequest(
            @JsonProperty("preferred_ingredients") List<String> preferredIngredients,
            @JsonProperty("disliked_ingredients") List<String> dislikedIngredients,
            @JsonProperty("preferred_categories") List<String> preferredCategories,
            @JsonProperty("dietary_tags") List<String> dietaryTags,
            List<String> allergens
    ) {
    }
}
