package com.babaai.core.service;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.DailySuggestion;
import com.babaai.core.domain.Ingredient;
import com.babaai.core.domain.Recipe;
import com.babaai.core.domain.RecipeIngredient;
import com.babaai.core.domain.UserPreferences;
import com.babaai.core.dto.RecipeDtos;
import com.babaai.core.repository.DailySuggestionRepository;
import com.babaai.core.repository.RecipeFavoriteRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deterministic, free-tier daily personalized suggestion engine (869dr0a4d). For each user it scores
 * the verified catalog by: pantry match %, a boost for recipes that use soon-to-expire pantry items
 * (the waste-reduction hero), taste-profile preference/favorite affinity, a disliked-ingredient
 * penalty, and a recency penalty for recently-suggested recipes; allergens/diet are hard-excluded
 * upstream by {@link DietGuard}. No LLM on this path. Results are persisted per user/day and served
 * all day (a personalized set is not globally cacheable).
 */
@Service
public class DailySuggestionService {

    private final RecipeCatalogCache catalogCache;
    private final IngredientService ingredientService;
    private final MatchingEngine matchingEngine;
    private final RecipeFavoriteRepository favoriteRepository;
    private final DailySuggestionRepository dailyRepository;
    private final PreferenceService preferenceService;
    private final DietGuard dietGuard;
    private final NotificationService notificationService;
    private final AppProperties appProperties;

    public DailySuggestionService(
            RecipeCatalogCache catalogCache,
            IngredientService ingredientService,
            MatchingEngine matchingEngine,
            RecipeFavoriteRepository favoriteRepository,
            DailySuggestionRepository dailyRepository,
            PreferenceService preferenceService,
            DietGuard dietGuard,
            NotificationService notificationService,
            AppProperties appProperties
    ) {
        this.catalogCache = catalogCache;
        this.ingredientService = ingredientService;
        this.matchingEngine = matchingEngine;
        this.favoriteRepository = favoriteRepository;
        this.dailyRepository = dailyRepository;
        this.preferenceService = preferenceService;
        this.dietGuard = dietGuard;
        this.notificationService = notificationService;
        this.appProperties = appProperties;
    }

    /** Returns today's persisted set, generating (and persisting) it lazily on first read of the day. */
    @Transactional
    public RecipeDtos.DailyPicksResponse today(UUID userId, LocalDate today, int limit) {
        List<DailySuggestion> rows = dailyRepository.findByUserIdAndGeneratedOnOrderByRankAsc(userId, today);
        if (rows.isEmpty()) {
            return generate(userId, today, limit, false);
        }
        return buildFromRows(userId, today, rows);
    }

    /** Computes, persists (replacing any existing set for the day), and optionally notifies. */
    @Transactional
    public RecipeDtos.DailyPicksResponse generate(UUID userId, LocalDate today, int limit, boolean notify) {
        AppProperties.Suggestions config = appProperties.getSuggestions();
        AppProperties.Weights w = config.getWeights();

        List<Recipe> catalog = catalogCache.snapshot();
        UserPreferences prefs = preferenceService.entityOrEmpty(userId);
        Set<String> excluded = dietGuard.excludedKeywordsFor(prefs);
        List<Recipe> safe = catalog.stream().filter(recipe -> dietGuard.isSafe(recipe, excluded)).toList();

        List<Ingredient> pantry = ingredientService.listUsable(userId);
        Set<String> expiringSoon = expiringSoonNames(pantry, today, config.getExpirySoonDays());

        Map<UUID, Integer> favoriteCounts = favoriteCounts();
        Set<UUID> favoriteIds = favoriteRepository.findRecipeIdsByUserId(userId);
        Set<String> favoriteCategories = new HashSet<>();
        for (Recipe recipe : catalog) {
            if (favoriteIds.contains(recipe.getId())) {
                favoriteCategories.add(recipe.getCategory().toLowerCase(Locale.ROOT));
            }
        }
        Set<UUID> recent = new HashSet<>(
                dailyRepository.findRecipeIdsSuggestedSince(userId, today.minusDays(config.getRecencyDays())));

        Set<String> preferredIngredients = normalizedSet(prefs.getPreferredIngredients());
        Set<String> dislikedIngredients = normalizedSet(prefs.getDislikedIngredients());
        Set<String> preferredCategories = normalizedSet(prefs.getPreferredCategories());

        Map<UUID, MatchingEngine.MatchResult> matchById = new HashMap<>();
        if (!pantry.isEmpty()) {
            for (MatchingEngine.MatchResult match : matchingEngine.matchRecipes(pantry, safe, 0, safe.size(), null)) {
                matchById.put(match.recipeId(), match);
            }
        }

        List<Scored> scored = new ArrayList<>();
        for (Recipe recipe : safe) {
            MatchingEngine.MatchResult match = matchById.get(recipe.getId());
            double matchPercent = match != null ? match.matchPercent() : 0.0;
            boolean canPrepare = match != null && match.canPrepare();
            List<String> missing = match != null ? match.missingIngredients() : List.of();
            List<String> used = match != null ? match.usedIngredients() : List.of();

            boolean usesExpiring = used.stream().anyMatch(name -> expiringSoon.contains(normalize(name)));
            boolean hasPreferred = anyIngredientMatches(recipe, preferredIngredients);
            boolean categoryPreferred = preferredCategories.contains(recipe.getCategory().toLowerCase(Locale.ROOT));
            boolean hasDisliked = anyIngredientMatches(recipe, dislikedIngredients);
            boolean favoriteCategory = favoriteCategories.contains(recipe.getCategory().toLowerCase(Locale.ROOT));
            boolean recentlySuggested = recent.contains(recipe.getId());
            int favoriteCount = favoriteCounts.getOrDefault(recipe.getId(), 0);
            double popularity = Math.min(recipe.getViewCount() + 3.0 * favoriteCount, 15.0);

            double score = w.getPantryMatch() * matchPercent
                    + (usesExpiring ? w.getExpirySoon() : 0.0)
                    + (hasPreferred ? w.getPreference() : 0.0)
                    + (categoryPreferred ? w.getPreference() * 0.5 : 0.0)
                    - (hasDisliked ? w.getDislike() : 0.0)
                    + (favoriteCategory ? w.getFavorite() : 0.0)
                    - (recentlySuggested ? w.getRecency() : 0.0)
                    + popularity;

            List<String> reasons = new ArrayList<>();
            if (usesExpiring) {
                reasons.add("expiring");
            }
            if (canPrepare) {
                reasons.add("ready");
            } else if (matchPercent >= 50) {
                reasons.add("almost");
            }
            if (hasPreferred || categoryPreferred || favoriteCategory) {
                reasons.add("taste");
            }
            if (favoriteCount > 0) {
                reasons.add("popular");
            }
            scored.add(new Scored(recipe, round(score), matchPercent, canPrepare, reasons, missing));
        }

        scored.sort((a, b) -> {
            int byScore = Double.compare(b.score, a.score);
            return byScore != 0 ? byScore : a.recipe.getName().compareToIgnoreCase(b.recipe.getName());
        });
        List<Scored> top = scored.stream().limit(Math.max(1, limit)).toList();

        persist(userId, today, top);
        if (notify && !top.isEmpty()) {
            notificationService.create(userId, "Today for you", notificationMessage(top), "suggestion");
        }
        return toResponse(userId, today, top, favoriteCounts, favoriteIds, pantry.isEmpty());
    }

    private void persist(UUID userId, LocalDate today, List<Scored> top) {
        dailyRepository.deleteByUserIdAndGeneratedOn(userId, today);
        int rank = 0;
        for (Scored item : top) {
            DailySuggestion row = new DailySuggestion();
            row.setUserId(userId);
            row.setGeneratedOn(today);
            row.setRecipeId(item.recipe.getId());
            row.setRecipeName(item.recipe.getName());
            row.setRank(rank++);
            row.setScore(item.score);
            row.setMatchPercent(item.matchPercent);
            row.setCanPrepare(item.canPrepare);
            row.setReasons(new ArrayList<>(item.reasons));
            row.setMissingIngredients(new ArrayList<>(item.missing));
            dailyRepository.save(row);
        }
    }

    private RecipeDtos.DailyPicksResponse buildFromRows(UUID userId, LocalDate today, List<DailySuggestion> rows) {
        Map<UUID, Recipe> byId = new HashMap<>();
        catalogCache.snapshot().forEach(recipe -> byId.put(recipe.getId(), recipe));
        Map<UUID, Integer> favoriteCounts = favoriteCounts();
        Set<UUID> favoriteIds = favoriteRepository.findRecipeIdsByUserId(userId);

        List<RecipeDtos.DailyPickResponse> items = new ArrayList<>();
        for (DailySuggestion row : rows) {
            Recipe recipe = byId.get(row.getRecipeId());
            if (recipe == null) {
                continue; // recipe was unverified/removed since generation — silently drop it
            }
            items.add(new RecipeDtos.DailyPickResponse(
                    DtoMapper.toRecipeResponse(recipe,
                            favoriteCounts.getOrDefault(recipe.getId(), 0),
                            favoriteIds.contains(recipe.getId())),
                    row.getMatchPercent(),
                    row.isCanPrepare(),
                    row.getScore(),
                    row.getReasons(),
                    row.getMissingIngredients()));
        }
        return new RecipeDtos.DailyPicksResponse(items, today, items.isEmpty() ? emptyMessage(true) : null);
    }

    private RecipeDtos.DailyPicksResponse toResponse(
            UUID userId,
            LocalDate today,
            List<Scored> top,
            Map<UUID, Integer> favoriteCounts,
            Set<UUID> favoriteIds,
            boolean pantryEmpty
    ) {
        List<RecipeDtos.DailyPickResponse> items = top.stream()
                .map(item -> new RecipeDtos.DailyPickResponse(
                        DtoMapper.toRecipeResponse(item.recipe,
                                favoriteCounts.getOrDefault(item.recipe.getId(), 0),
                                favoriteIds.contains(item.recipe.getId())),
                        item.matchPercent,
                        item.canPrepare,
                        item.score,
                        item.reasons,
                        item.missing))
                .toList();
        return new RecipeDtos.DailyPicksResponse(items, today, items.isEmpty() ? emptyMessage(pantryEmpty) : null);
    }

    private String emptyMessage(boolean pantryEmpty) {
        return pantryEmpty
                ? "Add ingredients to your pantry and set your taste profile to unlock personalised picks."
                : "No suggestions match your kitchen today — try adding pantry items or relaxing your filters.";
    }

    private String notificationMessage(List<Scored> top) {
        long expiring = top.stream().filter(s -> s.reasons.contains("expiring")).count();
        long ready = top.stream().filter(Scored::canPrepare).count();
        StringBuilder message = new StringBuilder(top.size() + " recipes picked for you today");
        if (expiring > 0) {
            message.append(" — ").append(expiring).append(" use ingredients expiring soon");
        } else if (ready > 0) {
            message.append(" — ").append(ready).append(" ready to cook now");
        }
        return message.append('.').toString();
    }

    private Set<String> expiringSoonNames(List<Ingredient> pantry, LocalDate today, int withinDays) {
        LocalDate cutoff = today.plusDays(withinDays);
        Set<String> names = new HashSet<>();
        for (Ingredient item : pantry) {
            LocalDate expiry = item.getExpirationDate();
            if (expiry != null && !expiry.isAfter(cutoff)) {
                names.add(normalize(item.getName()));
            }
        }
        return names;
    }

    private boolean anyIngredientMatches(Recipe recipe, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return false;
        }
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            String name = normalize(ingredient.getProductName());
            for (String keyword : keywords) {
                if (name.contains(keyword) || keyword.contains(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<String> normalizedSet(List<String> values) {
        Set<String> result = new HashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(normalize(value));
            }
        }
        return result;
    }

    private Map<UUID, Integer> favoriteCounts() {
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : favoriteRepository.countGroupedByRecipeId()) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    private String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record Scored(
            Recipe recipe,
            double score,
            double matchPercent,
            boolean canPrepare,
            List<String> reasons,
            List<String> missing
    ) {
    }
}
