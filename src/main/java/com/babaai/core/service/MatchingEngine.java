package com.babaai.core.service;

import com.babaai.core.domain.Ingredient;
import com.babaai.core.domain.Recipe;
import com.babaai.core.domain.RecipeIngredient;
import com.babaai.core.dto.RecipeDtos;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MatchingEngine {

    private final UnitConversion unitConversion;

    public MatchingEngine(UnitConversion unitConversion) {
        this.unitConversion = unitConversion;
    }

    public record MatchResult(
            UUID recipeId,
            String name,
            String preparation,
            double matchPercent,
            List<RecipeDtos.IngredientMatchDetailResponse> ingredients,
            boolean canPrepare,
            List<String> usedIngredients,
            List<String> missingIngredients
    ) {
    }

    public List<MatchResult> matchRecipes(
            List<Ingredient> pantry,
            List<Recipe> recipes,
            double minMatchPercent,
            int limit,
            Set<UUID> recipeIds
    ) {
        if (pantry.isEmpty()) {
            return List.of();
        }
        Map<String, List<Ingredient>> lookup = new HashMap<>();
        for (Ingredient item : pantry) {
            lookup.computeIfAbsent(normalizeName(item.getName()), key -> new ArrayList<>()).add(item);
        }

        List<MatchResult> results = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipeIds != null && !recipeIds.contains(recipe.getId())) {
                continue;
            }
            List<RecipeIngredient> requiredItems = recipe.getIngredients();
            if (requiredItems.isEmpty()) {
                continue;
            }
            List<RecipeDtos.IngredientMatchDetailResponse> details = requiredItems.stream()
                    .map(required -> evaluateIngredient(required, lookup))
                    .toList();
            long presentCount = details.stream().filter(d -> !"missing".equals(d.status())).count();
            double matchPercent = Math.round((presentCount * 1000.0 / details.size())) / 10.0;
            if (matchPercent < minMatchPercent) {
                continue;
            }
            boolean canPrepare = details.stream().allMatch(d -> "available".equals(d.status()));
            List<String> used = details.stream().filter(d -> !"missing".equals(d.status())).map(RecipeDtos.IngredientMatchDetailResponse::productName).toList();
            List<String> missing = details.stream().filter(d -> "missing".equals(d.status())).map(RecipeDtos.IngredientMatchDetailResponse::productName).toList();
            results.add(new MatchResult(
                    recipe.getId(),
                    recipe.getName(),
                    recipe.getPreparation(),
                    matchPercent,
                    details,
                    canPrepare,
                    used,
                    missing
            ));
        }

        results.sort(Comparator
                .comparing(MatchResult::matchPercent).reversed()
                .thenComparing(match -> match.missingIngredients().size())
                .thenComparing(match -> match.name().toLowerCase()));
        return results.stream().limit(limit).toList();
    }

    private RecipeDtos.IngredientMatchDetailResponse evaluateIngredient(
            RecipeIngredient required,
            Map<String, List<Ingredient>> pantryLookup
    ) {
        List<Ingredient> pantryItems = pantryLookup.getOrDefault(normalizeName(required.getProductName()), List.of());
        if (pantryItems.isEmpty()) {
            return new RecipeDtos.IngredientMatchDetailResponse(
                    required.getProductName(),
                    required.getQuantity(),
                    required.getUnit(),
                    null,
                    null,
                    "missing"
            );
        }

        UnitConversion.BaseQuantity requiredBase = unitConversion.toBaseQuantity(required.getQuantity(), required.getUnit());
        if (requiredBase == null) {
            Ingredient first = pantryItems.getFirst();
            return new RecipeDtos.IngredientMatchDetailResponse(
                    required.getProductName(),
                    required.getQuantity(),
                    required.getUnit(),
                    first.getQuantity(),
                    first.getUnit(),
                    "insufficient"
            );
        }

        double totalBase = 0.0;
        Double displayQuantity = null;
        String displayUnit = null;
        int compatibleCount = 0;
        for (Ingredient item : pantryItems) {
            UnitConversion.BaseQuantity itemBase = unitConversion.toBaseQuantity(item.getQuantity(), item.getUnit());
            if (itemBase == null || !itemBase.family().equals(requiredBase.family())) {
                continue;
            }
            compatibleCount++;
            totalBase += itemBase.amount();
            if (displayQuantity == null) {
                displayQuantity = item.getQuantity();
                displayUnit = item.getUnit();
            }
        }

        if (compatibleCount > 0 && displayUnit != null) {
            if (compatibleCount > 1) {
                displayQuantity = baseToDisplay(totalBase, requiredBase.family(), displayUnit);
            }
            String status = totalBase >= requiredBase.amount() ? "available" : "insufficient";
            return new RecipeDtos.IngredientMatchDetailResponse(
                    required.getProductName(),
                    required.getQuantity(),
                    required.getUnit(),
                    displayQuantity,
                    displayUnit,
                    status
            );
        }

        Ingredient first = pantryItems.getFirst();
        Boolean sufficient = unitConversion.isSufficient(
                required.getQuantity(),
                required.getUnit(),
                first.getQuantity(),
                first.getUnit()
        );
        String status = Boolean.TRUE.equals(sufficient) ? "available" : "insufficient";
        return new RecipeDtos.IngredientMatchDetailResponse(
                required.getProductName(),
                required.getQuantity(),
                required.getUnit(),
                first.getQuantity(),
                first.getUnit(),
                status
        );
    }

    private double baseToDisplay(double totalBase, String family, String preferredUnit) {
        UnitConversion.BaseQuantity unitBase = unitConversion.toBaseQuantity(1, preferredUnit);
        if (unitBase != null && unitBase.family().equals(family) && unitBase.amount() > 0) {
            return Math.round((totalBase / unitBase.amount()) * 1000.0) / 1000.0;
        }
        return totalBase;
    }

    private String normalizeName(String name) {
        return name.strip().toLowerCase();
    }
}
