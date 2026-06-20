package com.babaai.core.bootstrap;

import com.babaai.core.domain.IngredientCategory;
import com.babaai.core.domain.Recipe;
import com.babaai.core.domain.RecipeIngredient;
import com.babaai.core.repository.IngredientCategoryRepository;
import com.babaai.core.repository.RecipeRepository;
import com.babaai.core.service.JsonConfigService;
import com.babaai.core.service.NutritionCalculator;
import com.babaai.core.service.RecipeService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeedRunner.class);

    private final IngredientCategoryRepository categoryRepository;
    private final RecipeRepository recipeRepository;
    private final JsonConfigService jsonConfigService;
    private final NutritionCalculator nutritionCalculator;

    public DataSeedRunner(
            IngredientCategoryRepository categoryRepository,
            RecipeRepository recipeRepository,
            JsonConfigService jsonConfigService,
            NutritionCalculator nutritionCalculator
    ) {
        this.categoryRepository = categoryRepository;
        this.recipeRepository = recipeRepository;
        this.jsonConfigService = jsonConfigService;
        this.nutritionCalculator = nutritionCalculator;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int categoriesAdded = seedCategories();
        ensureDefaultIngredientCategory();
        int recipesAdded = seedRecipes();
        if (categoriesAdded > 0 || recipesAdded > 0) {
            log.info("Seed sync complete: {} ingredient categories added, {} recipes added", categoriesAdded, recipesAdded);
        }
    }

    private int seedCategories() {
        String defaultName = jsonConfigService.defaultIngredientCategoryName();
        int added = 0;
        for (Map<String, Object> definition : jsonConfigService.ingredientCategoryDefinitions()) {
            String name = String.valueOf(definition.get("name")).trim();
            if (name.isEmpty() || categoryRepository.findByName(name).isPresent()) {
                continue;
            }
            IngredientCategory category = new IngredientCategory();
            category.setName(name);
            Object keywords = definition.get("keywords");
            if (keywords instanceof List<?> list) {
                category.setKeywords(list.stream().map(String::valueOf).toList());
            }
            Object isDefault = definition.get("is_default");
            category.setDefault(Boolean.TRUE.equals(isDefault) || name.equalsIgnoreCase(defaultName));
            categoryRepository.save(category);
            added++;
        }
        return added;
    }

    private void ensureDefaultIngredientCategory() {
        if (categoryRepository.findFirstByIsDefaultTrue().isPresent()) {
            return;
        }
        String defaultName = jsonConfigService.defaultIngredientCategoryName();
        categoryRepository.findByName(defaultName).ifPresent(category -> {
            category.setDefault(true);
            categoryRepository.save(category);
            log.info("Marked ingredient category '{}' as default", defaultName);
        });
    }

    @SuppressWarnings("unchecked")
    private int seedRecipes() {
        int added = 0;
        for (Map<String, Object> raw : jsonConfigService.sampleRecipes()) {
            String name = String.valueOf(raw.get("name")).trim();
            if (name.isEmpty() || recipeRepository.existsByNameIgnoreCase(name)) {
                continue;
            }
            Recipe recipe = new Recipe();
            recipe.setName(name);
            recipe.setCategory(String.valueOf(raw.getOrDefault("category", "Other")));
            recipe.setPreparation(String.valueOf(raw.get("preparation")));
            recipe.setSourceType("seed");
            // Seeded recipes are trusted -> verified, so they actually surface in the catalog (869dqrre0).
            recipe.setVerified(RecipeService.verifiedFor(recipe.getSourceType()));
            Object servings = raw.get("servings");
            if (servings != null) {
                recipe.setServings((int) Math.round(Double.parseDouble(String.valueOf(servings))));
            }
            Object ingredients = raw.get("ingredients");
            if (ingredients instanceof List<?> list) {
                for (Object item : list) {
                    if (!(item instanceof Map<?, ?> map)) {
                        continue;
                    }
                    RecipeIngredient line = new RecipeIngredient();
                    line.setRecipe(recipe);
                    line.setProductName(String.valueOf(map.get("product_name")));
                    Object quantity = map.get("quantity");
                    line.setQuantity(quantity != null ? Double.parseDouble(String.valueOf(quantity)) : 1.0);
                    Object unit = map.get("unit");
                    line.setUnit(unit != null ? String.valueOf(unit) : "piece");
                    recipe.getIngredients().add(line);
                }
            }
            NutritionCalculator.Nutrition nutrition = nutritionCalculator.computeFromIngredients(recipe.getIngredients());
            recipe.setCalories(nutrition.calories());
            recipe.setProteinG(nutrition.proteinG());
            recipe.setCarbsG(nutrition.carbsG());
            recipe.setFatG(nutrition.fatG());
            recipe.setNutritionComplete(nutrition.complete());
            recipeRepository.save(recipe);
            added++;
        }
        return added;
    }
}
