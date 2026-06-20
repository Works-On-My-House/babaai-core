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
            @JsonProperty("is_favorite") boolean favorite,
            NutritionResponse nutrition
    ) {
    }

    /** Per-serving nutrition (recipe totals / servings). Macro fields are null until computed. */
    public record NutritionResponse(
            Double calories,
            @JsonProperty("protein_g") Double proteinG,
            @JsonProperty("carbs_g") Double carbsG,
            @JsonProperty("fat_g") Double fatG,
            boolean complete,
            Integer servings
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
            @JsonProperty("ai_proposals") List<AiRecipeProposal> aiProposals,
            // User-facing note when AI proposals are unavailable (e.g. the model failed); null otherwise.
            @JsonProperty("ai_message") String aiMessage
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

    /** Optional inline nutrition (recipe TOTAL) supplied by an import to bypass the curated reference. */
    public record NutritionInput(
            Double calories,
            @JsonProperty("protein_g") Double proteinG,
            @JsonProperty("carbs_g") Double carbsG,
            @JsonProperty("fat_g") Double fatG
    ) {
        public boolean hasAnyValue() {
            return calories != null || proteinG != null || carbsG != null || fatG != null;
        }
    }

    public record RecipeImportItem(
            String name,
            String category,
            String preparation,
            @JsonProperty("source_url") String sourceUrl,
            Integer servings,
            List<RecipeIngredientInput> ingredients,
            NutritionInput nutrition
    ) {
    }

    public record RecipeImportRequest(List<RecipeImportItem> recipes) {
    }

    public record RecipeImportResponse(
            int created,
            int skipped,
            @JsonProperty("created_names") List<String> createdNames,
            @JsonProperty("skipped_names") List<String> skippedNames
    ) {
    }

    public record ReindexRequest(@JsonProperty("recipe_id") UUID recipeId) {
    }
}
