package com.babaai.core.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "babaai")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Ai ai = new Ai();
    private final Gateway gateway = new Gateway();
    private final Defaults defaults = new Defaults();
    private final Config config = new Config();
    private final Cache cache = new Cache();

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public Ai getAi() {
        return ai;
    }

    public Gateway getGateway() {
        return gateway;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public Config getConfig() {
        return config;
    }

    public Cache getCache() {
        return cache;
    }

    public static class Jwt {
        private int expireMinutes;
        private String keyPath;
        private int refreshExpireDays = 30;
        private String refreshCookieName = "refresh_token";
        private boolean refreshCookieSecure = true;
        private String refreshCookieSameSite = "Lax";
        private String refreshCookiePath = "/";

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

        public int getRefreshExpireDays() {
            return refreshExpireDays;
        }

        public void setRefreshExpireDays(int refreshExpireDays) {
            this.refreshExpireDays = refreshExpireDays;
        }

        public String getRefreshCookieName() {
            return refreshCookieName;
        }

        public void setRefreshCookieName(String refreshCookieName) {
            this.refreshCookieName = refreshCookieName;
        }

        public boolean isRefreshCookieSecure() {
            return refreshCookieSecure;
        }

        public void setRefreshCookieSecure(boolean refreshCookieSecure) {
            this.refreshCookieSecure = refreshCookieSecure;
        }

        public String getRefreshCookieSameSite() {
            return refreshCookieSameSite;
        }

        public void setRefreshCookieSameSite(String refreshCookieSameSite) {
            this.refreshCookieSameSite = refreshCookieSameSite;
        }

        public String getRefreshCookiePath() {
            return refreshCookiePath;
        }

        public void setRefreshCookiePath(String refreshCookiePath) {
            this.refreshCookiePath = refreshCookiePath;
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

    public static class Gateway {
        private String baseUrl;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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

    public static class Cache {
        private boolean enabled = true;
        private final CacheSpec defaultSpec = new CacheSpec();
        private Map<String, CacheSpec> specs = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public CacheSpec getDefaultSpec() {
            return defaultSpec;
        }

        public Map<String, CacheSpec> getSpecs() {
            return specs;
        }

        public void setSpecs(Map<String, CacheSpec> specs) {
            this.specs = specs;
        }
    }

    public static class CacheSpec {
        private long maximumSize = 1_000;
        private Duration ttl = Duration.ofMinutes(10);

        public long getMaximumSize() {
            return maximumSize;
        }

        public void setMaximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }
}
