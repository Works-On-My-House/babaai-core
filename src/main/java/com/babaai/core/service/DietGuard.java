package com.babaai.core.service;

import com.babaai.core.domain.Recipe;
import com.babaai.core.domain.RecipeIngredient;
import com.babaai.core.domain.UserPreferences;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Hard safety filter for suggestions (869dr0a4d): excludes recipes whose ingredients violate a user's
 * allergens or dietary tags, by substring-matching ingredient names against the keyword lists in
 * {@code dietary_rules.json}. This is the "constraint-safety" value — allergens/diet are hard excludes,
 * unlike disliked ingredients which only penalize the score.
 */
@Component
public class DietGuard {

    private final JsonConfigService jsonConfigService;

    public DietGuard(JsonConfigService jsonConfigService) {
        this.jsonConfigService = jsonConfigService;
    }

    /** The set of ingredient keywords a recipe must NOT contain for this user (allergens + diets). */
    public Set<String> excludedKeywordsFor(UserPreferences prefs) {
        Set<String> excluded = new HashSet<>();
        if (prefs == null) {
            return excluded;
        }
        Map<String, List<String>> allergens = jsonConfigService.allergenKeywords();
        for (String allergen : prefs.getAllergens()) {
            excluded.addAll(allergens.getOrDefault(allergen.toLowerCase(Locale.ROOT), List.of()));
        }
        Map<String, List<String>> diets = jsonConfigService.dietExclusions();
        for (String diet : prefs.getDietaryTags()) {
            excluded.addAll(diets.getOrDefault(diet.toLowerCase(Locale.ROOT), List.of()));
        }
        return excluded;
    }

    /** True when none of the recipe's ingredients contain an excluded keyword. */
    public boolean isSafe(Recipe recipe, Set<String> excludedKeywords) {
        if (excludedKeywords.isEmpty()) {
            return true;
        }
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            String name = ingredient.getProductName().toLowerCase(Locale.ROOT);
            for (String keyword : excludedKeywords) {
                if (name.contains(keyword)) {
                    return false;
                }
            }
        }
        return true;
    }
}
