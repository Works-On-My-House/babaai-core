package com.babaai.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public final class CookedDtos {

    private CookedDtos() {
    }

    /** Optional servings actually cooked; scales the pantry decrement relative to the recipe's servings. */
    public record CookedRequest(Integer servings) {
    }

    public record ConsumedLineResponse(
            @JsonProperty("product_name") String productName,
            Double quantity,
            String unit
    ) {
    }

    public record CookedResponse(
            @JsonProperty("recipe_id") UUID recipeId,
            @JsonProperty("recipe_name") String recipeName,
            List<ConsumedLineResponse> consumed,
            // Recipe lines that couldn't be auto-deducted (no pantry match or incompatible unit).
            @JsonProperty("unmatched_ingredients") List<String> unmatchedIngredients
    ) {
    }
}
