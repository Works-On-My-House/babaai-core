package com.babaai.core.service;

import com.babaai.core.domain.BaseEntity;
import com.babaai.core.domain.Ingredient;
import com.babaai.core.domain.IngredientCategory;
import com.babaai.core.domain.Notification;
import com.babaai.core.domain.Recipe;
import com.babaai.core.domain.RecipeIngredient;
import com.babaai.core.domain.SuggestionHistory;
import com.babaai.core.domain.User;
import com.babaai.core.dto.IngredientDtos;
import com.babaai.core.dto.NotificationDtos;
import com.babaai.core.dto.RecipeDtos;
import com.babaai.core.dto.Dtos;
import java.util.List;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static Dtos.UserResponse toUserResponse(User user) {
        return new Dtos.UserResponse(
                user.getId(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getVersion(),
                user.getEmail(),
                user.getUsername()
        );
    }

    public static IngredientDtos.IngredientResponse toIngredientResponse(Ingredient ingredient, String categoryName) {
        return new IngredientDtos.IngredientResponse(
                ingredient.getId(),
                ingredient.getCreatedAt(),
                ingredient.getUpdatedAt(),
                ingredient.getVersion(),
                ingredient.getName(),
                ingredient.getCategoryId(),
                categoryName,
                ingredient.getQuantity(),
                ingredient.getUnit(),
                ingredient.getExpirationDate(),
                ingredient.getNotes(),
                ingredient.getUserId()
        );
    }

    public static RecipeDtos.RecipeIngredientResponse toRecipeIngredientResponse(RecipeIngredient ingredient) {
        return new RecipeDtos.RecipeIngredientResponse(
                ingredient.getId(),
                ingredient.getCreatedAt(),
                ingredient.getUpdatedAt(),
                ingredient.getVersion(),
                ingredient.getProductName(),
                ingredient.getQuantity(),
                ingredient.getUnit()
        );
    }

    public static RecipeDtos.RecipeResponse toRecipeResponse(Recipe recipe) {
        return toRecipeResponse(recipe, 0, false);
    }

    public static RecipeDtos.RecipeResponse toRecipeResponse(Recipe recipe, int favoriteCount, boolean favorite) {
        List<RecipeDtos.RecipeIngredientResponse> ingredients = recipe.getIngredients().stream()
                .map(DtoMapper::toRecipeIngredientResponse)
                .toList();
        return new RecipeDtos.RecipeResponse(
                recipe.getId(),
                recipe.getCreatedAt(),
                recipe.getUpdatedAt(),
                recipe.getVersion(),
                recipe.getName(),
                recipe.getCategory(),
                recipe.getPreparation(),
                ingredients,
                recipe.getViewCount(),
                favoriteCount,
                favorite
        );
    }

    public static NotificationDtos.NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationDtos.NotificationResponse(
                notification.getId(),
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                notification.getVersion(),
                notification.getUserId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead()
        );
    }

    public static RecipeDtos.SuggestionHistoryResponse toHistoryResponse(SuggestionHistory history) {
        List<RecipeDtos.HistoryIngredientResponse> ingredients = history.getIngredients() == null
                ? List.of()
                : history.getIngredients().stream()
                        .map(entry -> new RecipeDtos.HistoryIngredientResponse(
                                entry.getProductName(),
                                entry.getQuantity(),
                                entry.getUnit()
                        ))
                        .toList();
        return new RecipeDtos.SuggestionHistoryResponse(
                history.getId(),
                history.getCreatedAt(),
                history.getUpdatedAt(),
                history.getVersion(),
                history.getUserId(),
                history.getRecipeId(),
                history.getRecipeName(),
                history.getMatchPercent(),
                history.getUsedIngredients(),
                history.getMissingIngredients(),
                history.getSource(),
                history.getAgentLabel(),
                history.getPreparation(),
                ingredients
        );
    }

    public static <T extends BaseEntity> int pages(int total, int pageSize) {
        if (pageSize <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / pageSize);
    }
}
