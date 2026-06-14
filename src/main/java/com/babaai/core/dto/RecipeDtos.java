package com.babaai.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class RecipeDtos {

    private RecipeDtos() {
    }

    public record RecipeIngredientResponse(
            UUID id,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt,
            Integer version,
            @JsonProperty("product_name") String productName,
            double quantity,
            String unit
    ) {
    }

    public record RecipeResponse(
            UUID id,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt,
            Integer version,
            String name,
            String category,
            String preparation,
            List<RecipeIngredientResponse> ingredients,
            @JsonProperty("view_count") int viewCount,
            @JsonProperty("favorite_count") int favoriteCount,
            @JsonProperty("is_favorite") boolean favorite
    ) {
    }

    public record RecipeListResponse(
            List<RecipeResponse> items,
            int total,
            int page,
            @JsonProperty("page_size") int pageSize,
            int pages
    ) {
    }

    public record RecipeCategoriesResponse(List<String> categories) {
    }

    public record IngredientMatchDetailResponse(
            @JsonProperty("product_name") String productName,
            @JsonProperty("required_quantity") double requiredQuantity,
            @JsonProperty("required_unit") String requiredUnit,
            @JsonProperty("available_quantity") Double availableQuantity,
            @JsonProperty("available_unit") String availableUnit,
            String status
    ) {
    }

    public record SuggestionRequest(
            @JsonProperty("min_match_percent") Double minMatchPercent,
            Integer limit,
            @JsonProperty("ingredient_ids") List<UUID> ingredientIds,
            @JsonProperty("recipe_ids") List<UUID> recipeIds,
            @JsonProperty("include_ai") Boolean includeAi
    ) {
        public boolean includeAiOrDefault() {
            return includeAi == null || includeAi;
        }
    }

    public record AiProposalIngredient(
            @JsonProperty("product_name") String productName,
            Double quantity,
            String unit
    ) {
    }

    public record AiRecipeProposal(
            String name,
            String category,
            String preparation,
            List<String> steps,
            List<AiProposalIngredient> ingredients,
            @JsonProperty("agent_id") String agentId,
            @JsonProperty("agent_label") String agentLabel
    ) {
    }

    public record SuggestionItemResponse(
            @JsonProperty("recipe_id") UUID recipeId,
            String name,
            String preparation,
            @JsonProperty("match_percent") double matchPercent,
            List<IngredientMatchDetailResponse> ingredients,
            @JsonProperty("used_ingredients") List<String> usedIngredients,
            @JsonProperty("missing_ingredients") List<String> missingIngredients,
            @JsonProperty("can_prepare") boolean canPrepare
    ) {
    }

    public record SuggestionResponse(
            List<SuggestionItemResponse> suggestions,
            int total,
            int page,
            @JsonProperty("page_size") int pageSize,
            int pages,
            String message,
            @JsonProperty("ai_proposals") List<AiRecipeProposal> aiProposals
    ) {
    }

    public record HistoryIngredientResponse(
            @JsonProperty("product_name") String productName,
            Double quantity,
            String unit
    ) {
    }

    public record SuggestionHistoryResponse(
            UUID id,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt,
            Integer version,
            @JsonProperty("user_id") UUID userId,
            @JsonProperty("recipe_id") UUID recipeId,
            @JsonProperty("recipe_name") String recipeName,
            @JsonProperty("match_percent") Double matchPercent,
            @JsonProperty("used_ingredients") List<String> usedIngredients,
            @JsonProperty("missing_ingredients") List<String> missingIngredients,
            String source,
            @JsonProperty("agent_label") String agentLabel,
            String preparation,
            List<HistoryIngredientResponse> ingredients
    ) {
    }

    public record SuggestionHistoryListResponse(
            List<SuggestionHistoryResponse> items,
            int total,
            int page,
            @JsonProperty("page_size") int pageSize,
            int pages
    ) {
    }

    public record FavoriteResponse(
            @JsonProperty("recipe_id") UUID recipeId,
            @JsonProperty("is_favorite") boolean favorite,
            @JsonProperty("favorite_count") int favoriteCount
    ) {
    }

    public record FavoriteListResponse(List<RecipeResponse> items, int total) {
    }

    public record DailyPickResponse(
            RecipeResponse recipe,
            @JsonProperty("match_percent") double matchPercent,
            @JsonProperty("can_prepare") boolean canPrepare,
            double score,
            List<String> reasons,
            @JsonProperty("missing_ingredients") List<String> missingIngredients
    ) {
    }

    public record DailyPicksResponse(
            List<DailyPickResponse> items,
            @JsonProperty("generated_for") LocalDate generatedFor,
            String message
    ) {
    }

    public record RecipeIngredientInput(
            @JsonProperty("product_name") String productName,
            double quantity,
            String unit
    ) {
        public RecipeIngredientInput {
            if (quantity <= 0) {
                quantity = 1.0;
            }
        }
    }

    public record RecipeIngestRequest(
            String name,
            String category,
            String preparation,
            @JsonProperty("source_url") String sourceUrl,
            List<RecipeIngredientInput> ingredients
    ) {
    }

    public record RecipeIngestResponse(
            @JsonProperty("recipe_id") UUID recipeId,
            String name,
            boolean created
    ) {
    }

    public record ReindexRequest(UUID recipeId) {
    }
}
