package com.babaai.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "babaai")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Ai ai = new Ai();
    private final Defaults defaults = new Defaults();
    private final Config config = new Config();

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public Ai getAi() {
        return ai;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public Config getConfig() {
        return config;
    }

    public static class Jwt {
        private int expireMinutes;
        private String keyPath;

        public int getExpireMinutes() {
            return expireMinutes;
        }

        public void setExpireMinutes(int expireMinutes) {
            this.expireMinutes = expireMinutes;
        }

        public String getKeyPath() {
            return keyPath;
        }

        public void setKeyPath(String keyPath) {
            this.keyPath = keyPath;
        }
    }

    public static class Cors {
        private String allowedOrigins;

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Ai {
        private String baseUrl;
        private String serviceToken;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getServiceToken() {
            return serviceToken;
        }

        public void setServiceToken(String serviceToken) {
            this.serviceToken = serviceToken;
        }
    }

    public static class Defaults {
        private double minMatchPercent;
        private int suggestionLimit;
        private int pageSize;
        private String ingredientCategory;

        public double getMinMatchPercent() {
            return minMatchPercent;
        }

        public void setMinMatchPercent(double minMatchPercent) {
            this.minMatchPercent = minMatchPercent;
        }

        public int getSuggestionLimit() {
            return suggestionLimit;
        }

        public void setSuggestionLimit(int suggestionLimit) {
            this.suggestionLimit = suggestionLimit;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public String getIngredientCategory() {
            return ingredientCategory;
        }

        public void setIngredientCategory(String ingredientCategory) {
            this.ingredientCategory = ingredientCategory;
        }
    }

    public static class Config {
        private String ingredientCategoriesPath = "classpath:config/ingredient_categories.json";
        private String unitConversionPath = "classpath:config/unit_conversion.json";
        private String sampleRecipesPath = "classpath:config/sample_recipes.json";
        private String appDefaultsPath = "classpath:config/app_defaults.json";
        private String recipeCategoriesPath = "classpath:config/recipe_categories.json";

        public String getIngredientCategoriesPath() {
            return ingredientCategoriesPath;
        }

        public void setIngredientCategoriesPath(String ingredientCategoriesPath) {
            this.ingredientCategoriesPath = ingredientCategoriesPath;
        }

        public String getUnitConversionPath() {
            return unitConversionPath;
        }

        public void setUnitConversionPath(String unitConversionPath) {
            this.unitConversionPath = unitConversionPath;
        }

        public String getSampleRecipesPath() {
            return sampleRecipesPath;
        }

        public void setSampleRecipesPath(String sampleRecipesPath) {
            this.sampleRecipesPath = sampleRecipesPath;
        }

        public String getAppDefaultsPath() {
            return appDefaultsPath;
        }

        public void setAppDefaultsPath(String appDefaultsPath) {
            this.appDefaultsPath = appDefaultsPath;
        }

        public String getRecipeCategoriesPath() {
            return recipeCategoriesPath;
        }

        public void setRecipeCategoriesPath(String recipeCategoriesPath) {
            this.recipeCategoriesPath = recipeCategoriesPath;
        }
    }
}
