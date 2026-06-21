package com.babaai.core.service;

import com.babaai.core.config.AppProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class JsonConfigService {

    private final JsonConfigCache jsonConfigCache;
    private final AppProperties appProperties;

    public JsonConfigService(JsonConfigCache jsonConfigCache, AppProperties appProperties) {
        this.jsonConfigCache = jsonConfigCache;
        this.appProperties = appProperties;
    }

    // Delegates to the cached loader (PERF-1.4) — file IO + parse happens once per location.
    private Map<String, Object> loadObject(String location) {
        return jsonConfigCache.loadObject(location);
    }

    private Map<String, Object> unitConversionConfig() {
        return loadObject(appProperties.getConfig().getUnitConversionPath());
    }

    private Map<String, Object> appDefaultsConfig() {
        return loadObject(appProperties.getConfig().getAppDefaultsPath());
    }

    private Map<String, Object> ingredientCategoriesConfig() {
        return loadObject(appProperties.getConfig().getIngredientCategoriesPath());
    }

    public Map<String, Double> massToGrams() {
        return toDoubleMap(unitConversionConfig().get("mass_to_grams"));
    }

    public Map<String, Double> volumeToMl() {
        return toDoubleMap(unitConversionConfig().get("volume_to_ml"));
    }

    public Set<String> countUnits() {
        return new LinkedHashSet<>(toStringList(unitConversionConfig().get("count_units")));
    }

    public Map<String, String> unitAliases() {
        return toStringMap(unitConversionConfig().get("unit_aliases"));
    }

    public List<String> ingredientUnits() {
        return toStringList(appDefaultsConfig().get("ingredient_units"));
    }

    public List<Map<String, Object>> sampleRecipes() {
        return toMapList(loadObject(appProperties.getConfig().getSampleRecipesPath()).get("recipes"));
    }

    public List<String> recipeCategories() {
        return toStringList(loadObject(appProperties.getConfig().getRecipeCategoriesPath()).get("categories"));
    }

    public List<Map<String, Object>> ingredientCategoryDefinitions() {
        return toMapList(ingredientCategoriesConfig().get("categories"));
    }

    private Map<String, Object> ingredientNutritionConfig() {
        return loadObject(appProperties.getConfig().getIngredientNutritionPath());
    }

    /** Canonical ingredient -> nutrition entry ({@code per_100g}, optional {@code grams_per_piece}/{@code density_g_per_ml}). */
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> nutritionReference() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        Object ingredients = ingredientNutritionConfig().get("ingredients");
        if (ingredients instanceof Map<?, ?> map) {
            map.forEach((key, value) -> {
                if (value instanceof Map<?, ?> entry) {
                    result.put(String.valueOf(key).strip().toLowerCase(), (Map<String, Object>) entry);
                }
            });
        }
        return result;
    }

    /** Alias -> canonical ingredient name for the nutrition reference. */
    public Map<String, String> nutritionAliases() {
        Map<String, String> result = new HashMap<>();
        toStringMap(ingredientNutritionConfig().get("aliases"))
                .forEach((key, value) -> result.put(key.strip().toLowerCase(), value.strip().toLowerCase()));
        return result;
    }

    private Map<String, Object> dietaryRulesConfig() {
        return loadObject(appProperties.getConfig().getDietaryRulesPath());
    }

    /** Allergen name -> lowercase ingredient keywords that mark a recipe unsafe for that allergen. */
    public Map<String, List<String>> allergenKeywords() {
        return toKeywordMap(dietaryRulesConfig().get("allergens"));
    }

    /** Dietary tag -> lowercase ingredient keywords a recipe must NOT contain to satisfy that diet. */
    public Map<String, List<String>> dietExclusions() {
        return toKeywordMap(dietaryRulesConfig().get("diets"));
    }

    private Map<String, List<String>> toKeywordMap(Object value) {
        Map<String, List<String>> result = new HashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, val) -> result.put(
                    String.valueOf(key).strip().toLowerCase(),
                    toStringList(val).stream().map(s -> s.strip().toLowerCase()).toList()));
        }
        return result;
    }

    public String defaultIngredientCategoryName() {
        Object value = ingredientCategoriesConfig().get("default_category");
        if (value != null) {
            String name = String.valueOf(value).trim();
            if (!name.isEmpty()) {
                return name;
            }
        }
        return appProperties.getDefaults().getIngredientCategory();
    }

    private Map<String, Double> toDoubleMap(Object value) {
        Map<String, Double> result = new HashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, val) -> result.put(String.valueOf(key), Double.parseDouble(String.valueOf(val))));
        }
        return result;
    }

    private Map<String, String> toStringMap(Object value) {
        Map<String, String> result = new HashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, val) -> result.put(String.valueOf(key), String.valueOf(val)));
        }
        return result;
    }

    private List<String> toStringList(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            list.forEach(item -> result.add(String.valueOf(item)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMapList(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
        }
        return result;
    }
}
