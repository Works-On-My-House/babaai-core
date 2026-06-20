package com.babaai.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.Recipe;
import com.babaai.core.domain.RecipeIngredient;
import com.babaai.core.domain.UserPreferences;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.json.JsonMapper;

/** Allergen + dietary hard-filtering against the real dietary_rules.json. */
class DietGuardTest {

    private DietGuard newGuard() {
        AppProperties props = new AppProperties();
        JsonConfigCache cache = new JsonConfigCache(new DefaultResourceLoader(), JsonMapper.builder().build());
        JsonConfigService config = new JsonConfigService(cache, props);
        return new DietGuard(config);
    }

    private Recipe recipe(String... ingredientNames) {
        Recipe recipe = new Recipe();
        recipe.setName("Test");
        for (String name : ingredientNames) {
            RecipeIngredient line = new RecipeIngredient();
            line.setRecipe(recipe);
            line.setProductName(name);
            line.setQuantity(1);
            line.setUnit("g");
            recipe.getIngredients().add(line);
        }
        return recipe;
    }

    @Test
    void shellfishAllergenExcludesShellfishRecipes() {
        DietGuard guard = newGuard();
        UserPreferences prefs = new UserPreferences();
        prefs.setAllergens(List.of("shellfish"));
        Set<String> excluded = guard.excludedKeywordsFor(prefs);

        assertThat(guard.isSafe(recipe("Shrimp", "Garlic"), excluded)).isFalse();
        assertThat(guard.isSafe(recipe("Mussels", "Butter"), excluded)).isFalse();
        assertThat(guard.isSafe(recipe("Chicken", "Rice"), excluded)).isTrue();
    }

    @Test
    void veganExcludesMeatAndDairy() {
        DietGuard guard = newGuard();
        UserPreferences prefs = new UserPreferences();
        prefs.setDietaryTags(List.of("vegan"));
        Set<String> excluded = guard.excludedKeywordsFor(prefs);

        assertThat(guard.isSafe(recipe("Tofu", "Rice", "Soy Sauce"), excluded)).isTrue();
        assertThat(guard.isSafe(recipe("Beef", "Onion"), excluded)).isFalse();
        assertThat(guard.isSafe(recipe("Cheese", "Bread"), excluded)).isFalse();
    }

    @Test
    void noPreferencesMeansEverythingIsSafe() {
        DietGuard guard = newGuard();
        Set<String> excluded = guard.excludedKeywordsFor(new UserPreferences());
        assertThat(excluded).isEmpty();
        assertThat(guard.isSafe(recipe("Shrimp", "Beef", "Cheese"), excluded)).isTrue();
    }
}
