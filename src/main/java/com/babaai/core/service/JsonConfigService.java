package com.babaai.core.service;

import com.babaai.core.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonConfigService {

    private final ResourceLoader resourceLoader;
    private final JsonMapper jsonMapper;
    private final AppProperties appProperties;

    public JsonConfigService(
            ResourceLoader resourceLoader,
            JsonMapper jsonMapper,
            AppProperties appProperties
    ) {
        this.resourceLoader = resourceLoader;
        this.jsonMapper = jsonMapper;
        this.appProperties = appProperties;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadObject(String location) {
        Resource resource = resourceLoader.getResource(location);
        try (InputStream inputStream = resource.getInputStream()) {
            return jsonMapper.readValue(inputStream, Map.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load config: " + location, ex);
        }
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
