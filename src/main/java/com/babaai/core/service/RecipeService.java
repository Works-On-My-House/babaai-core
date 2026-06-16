package com.babaai.core.service;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.Ingredient;
import com.babaai.core.domain.Recipe;
import com.babaai.core.domain.RecipeFavorite;
import com.babaai.core.domain.RecipeIngredient;
import com.babaai.core.domain.SuggestionHistory;
import com.babaai.core.dto.RecipeDtos;
import com.babaai.core.exception.AppException;
import com.babaai.core.exception.NotFoundException;
import com.babaai.core.repository.RecipeFavoriteRepository;
import com.babaai.core.repository.RecipeRepository;
import com.babaai.core.repository.RecipeSpecifications;
import com.babaai.core.repository.SuggestionHistoryRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {

    private static final int FEATURED_POOL_SIZE = 10;

    private final RecipeRepository recipeRepository;
    private final RecipeCatalogCache recipeCatalogCache;
    private final RecipeFavoriteRepository favoriteRepository;
    private final SuggestionHistoryRepository historyRepository;
    private final IngredientService ingredientService;
    private final MatchingEngine matchingEngine;
    private final AiServiceClient aiServiceClient;
    private final AppProperties appProperties;
    private final JsonConfigService jsonConfigService;

    public RecipeService(
            RecipeRepository recipeRepository,
            RecipeCatalogCache recipeCatalogCache,
            RecipeFavoriteRepository favoriteRepository,
            SuggestionHistoryRepository historyRepository,
            IngredientService ingredientService,
            MatchingEngine matchingEngine,
            AiServiceClient aiServiceClient,
            AppProperties appProperties,
            JsonConfigService jsonConfigService
    ) {
        this.recipeRepository = recipeRepository;
        this.recipeCatalogCache = recipeCatalogCache;
        this.favoriteRepository = favoriteRepository;
        this.historyRepository = historyRepository;
        this.ingredientService = ingredientService;
        this.matchingEngine = matchingEngine;
        this.aiServiceClient = aiServiceClient;
        this.appProperties = appProperties;
        this.jsonConfigService = jsonConfigService;
    }

    @Transactional(readOnly = true)
    public RecipeDtos.RecipeListResponse list(int page, int pageSize, String search, String category, UUID userId) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Specification<Recipe> spec = RecipeSpecifications.search(search, category);
        Page<Recipe> result = recipeRepository.findAll(
                spec,
                PageRequest.of(safePage - 1, safePageSize, Sort.by("name"))
        );
        FavoriteView fav = favoriteView(result.getContent(), userId);
        return new RecipeDtos.RecipeListResponse(
                result.getContent().stream().map(recipe -> mapRecipe(recipe, fav)).toList(),
                (int) result.getTotalElements(),
                safePage,
                safePageSize,
                DtoMapper.pages((int) result.getTotalElements(), safePageSize)
        );
    }

    @Transactional(readOnly = true)
    public RecipeDtos.RecipeResponse get(UUID recipeId, UUID userId) {
        Recipe recipe = recipeRepository.findWithIngredientsById(recipeId)
                .orElseThrow(() -> new NotFoundException("Recipe not found"));
        return mapRecipe(recipe, favoriteView(List.of(recipe), userId));
    }

    @Transactional
    public RecipeDtos.RecipeResponse recordView(UUID recipeId, UUID userId) {
        Recipe recipe = recipeRepository.findWithIngredientsById(recipeId)
                .orElseThrow(() -> new NotFoundException("Recipe not found"));
        recipe.setViewCount(recipe.getViewCount() + 1);
        recipeRepository.save(recipe);
        // A view only bumps view_count; the cached catalog's ranking absorbs that staleness via TTL
        // (no eviction here — views are far too frequent to evict on).
        return mapRecipe(recipe, favoriteView(List.of(recipe), userId));
    }

    @Transactional(readOnly = true)
    public RecipeDtos.RecipeCategoriesResponse listCategories() {
        List<String> ordered = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String category : jsonConfigService.recipeCategories()) {
            if (category != null && !category.isBlank() && seen.add(category)) {
                ordered.add(category);
            }
        }
        List<String> used = new ArrayList<>(recipeRepository.findDistinctCategories());
        used.sort(Comparator.naturalOrder());
        for (String category : used) {
            if (category != null && !category.isBlank() && seen.add(category)) {
                ordered.add(category);
            }
        }
        return new RecipeDtos.RecipeCategoriesResponse(ordered);
    }

    @Transactional(readOnly = true)
    public RecipeDtos.RecipeResponse featured(UUID userId, LocalDate today) {
        List<Recipe> recipes = recipeCatalogCache.snapshot();
        if (recipes.isEmpty()) {
            return null;
        }
        Map<UUID, Integer> favoriteCounts = favoriteCounts();
        List<Recipe> ranked = recipes.stream()
                .sorted(Comparator.comparingDouble((Recipe r) -> -popularity(r, favoriteCounts))
                        .thenComparing(r -> r.getName().toLowerCase()))
                .toList();
        int poolSize = Math.min(FEATURED_POOL_SIZE, ranked.size());
        Recipe chosen = ranked.get((int) (today.toEpochDay() % poolSize));
        return mapRecipe(chosen, favoriteView(List.of(chosen), userId));
    }

    @Transactional(readOnly = true)
    public RecipeDtos.DailyPicksResponse dailyPicks(UUID userId, LocalDate today, int limit) {
        List<Recipe> recipes = recipeCatalogCache.snapshot();
        if (recipes.isEmpty()) {
            return new RecipeDtos.DailyPicksResponse(List.of(), today, "No recipes are available yet.");
        }
        List<Ingredient> pantry = ingredientService.listUsable(userId);
        Map<UUID, Integer> favoriteCounts = favoriteCounts();
        Set<UUID> favoriteIds = favoriteRepository.findRecipeIdsByUserId(userId);
        Set<String> preferredCategories = new HashSet<>();
        for (Recipe recipe : recipes) {
            if (favoriteIds.contains(recipe.getId())) {
                preferredCategories.add(recipe.getCategory());
            }
        }

        List<MatchingEngine.MatchResult> matches = pantry.isEmpty()
                ? List.of()
                : matchingEngine.matchRecipes(pantry, recipes, 1, recipes.size(), null);

        if (matches.isEmpty()) {
            return popularFallback(recipes, favoriteCounts, userId, today, limit, pantry.size());
        }

        Map<UUID, Recipe> recipeById = new HashMap<>();
        recipes.forEach(recipe -> recipeById.put(recipe.getId(), recipe));
        List<RecipeDtos.DailyPickResponse> items = new ArrayList<>();
        for (MatchingEngine.MatchResult match : matches) {
            Recipe recipe = recipeById.get(match.recipeId());
            if (recipe == null) {
                continue;
            }
            double tasteBoost = preferredCategories.contains(recipe.getCategory()) ? 20.0 : 0.0;
            double popularity = Math.min(popularity(recipe, favoriteCounts), 15.0);
            double readyBoost = match.canPrepare() ? 10.0 : 0.0;
            double score = Math.round((match.matchPercent() + tasteBoost + popularity + readyBoost) * 10.0) / 10.0;
            List<String> reasons = new ArrayList<>();
            if (match.canPrepare()) {
                reasons.add("ready");
            } else if (match.matchPercent() >= 50) {
                reasons.add("almost");
            }
            if (preferredCategories.contains(recipe.getCategory())) {
                reasons.add("taste");
            }
            if (favoriteCounts.getOrDefault(recipe.getId(), 0) > 0) {
                reasons.add("popular");
            }
            items.add(new RecipeDtos.DailyPickResponse(
                    DtoMapper.toRecipeResponse(
                            recipe,
                            favoriteCounts.getOrDefault(recipe.getId(), 0),
                            favoriteIds.contains(recipe.getId())),
                    match.matchPercent(),
                    match.canPrepare(),
                    score,
                    reasons,
                    match.missingIngredients()
            ));
        }
        long seed = today.toEpochDay();
        items.sort(Comparator.comparingDouble(RecipeDtos.DailyPickResponse::score).reversed()
                .thenComparing(item -> (item.recipe().id().getMostSignificantBits() + seed) % 9973));
        return new RecipeDtos.DailyPicksResponse(items.stream().limit(limit).toList(), today, null);
    }

    @Transactional
    public RecipeDtos.FavoriteResponse addFavorite(UUID userId, UUID recipeId) {
        get(recipeId, userId);
        if (favoriteRepository.findByUserIdAndRecipeId(userId, recipeId).isEmpty()) {
            RecipeFavorite favorite = new RecipeFavorite();
            favorite.setUserId(userId);
            favorite.setRecipeId(recipeId);
            favoriteRepository.save(favorite);
        }
        int count = favoriteCounts(List.of(recipeId)).getOrDefault(recipeId, 0);
        return new RecipeDtos.FavoriteResponse(recipeId, true, count);
    }

    @Transactional
    public RecipeDtos.FavoriteResponse removeFavorite(UUID userId, UUID recipeId) {
        favoriteRepository.deleteByUserIdAndRecipeId(userId, recipeId);
        int count = favoriteCounts(List.of(recipeId)).getOrDefault(recipeId, 0);
        return new RecipeDtos.FavoriteResponse(recipeId, false, count);
    }

    @Transactional(readOnly = true)
    public RecipeDtos.FavoriteListResponse listFavorites(UUID userId) {
        Set<UUID> ids = favoriteRepository.findRecipeIdsByUserId(userId);
        List<Recipe> withIngredients = ids.stream()
                .map(id -> recipeRepository.findWithIngredientsById(id).orElse(null))
                .filter(recipe -> recipe != null)
                .sorted(Comparator.comparing(r -> r.getName().toLowerCase()))
                .toList();
        FavoriteView fav = favoriteView(withIngredients, userId);
        return new RecipeDtos.FavoriteListResponse(
                withIngredients.stream().map(recipe -> mapRecipe(recipe, fav)).toList(),
                withIngredients.size()
        );
    }

    @Transactional
    public RecipeDtos.SuggestionResponse generateSuggestions(UUID userId, RecipeDtos.SuggestionRequest request) {
        double minMatch = request.minMatchPercent() != null
                ? request.minMatchPercent()
                : appProperties.getDefaults().getMinMatchPercent();
        int limit = request.limit() != null ? request.limit() : appProperties.getDefaults().getSuggestionLimit();

        List<Ingredient> pantry = request.ingredientIds() != null && !request.ingredientIds().isEmpty()
                ? ingredientService.listByIds(userId, request.ingredientIds())
                : ingredientService.listUsable(userId);
        List<Ingredient> usable = pantry.stream()
                .filter(item -> item.getExpirationDate() == null || !item.getExpirationDate().isBefore(LocalDate.now()))
                .toList();
        List<Recipe> recipes = recipeCatalogCache.snapshot();
        List<String> pantryNames = usable.stream().map(Ingredient::getName).toList();

        AiServiceClient.AiProposals aiResult = request.includeAiOrDefault()
                ? aiServiceClient.fetchProposals(pantryNames, 3)
                : AiServiceClient.AiProposals.empty();
        List<RecipeDtos.AiRecipeProposal> aiProposals = aiResult.proposals();
        String aiMessage = aiResult.message();
        if (!aiProposals.isEmpty()) {
            saveAiHistory(userId, aiProposals);
        }

        if (pantry.isEmpty()) {
            return new RecipeDtos.SuggestionResponse(
                    List.of(), 0, 1, limit, 0,
                    "Add ingredients to your pantry before generating suggestions.",
                    aiProposals, aiMessage
            );
        }
        if (usable.isEmpty()) {
            return new RecipeDtos.SuggestionResponse(
                    List.of(), 0, 1, limit, 0,
                    "All selected pantry ingredients are expired. Update expiration dates or add fresh items to generate suggestions.",
                    aiProposals, aiMessage
            );
        }
        if (recipes.isEmpty()) {
            return new RecipeDtos.SuggestionResponse(
                    List.of(), 0, 1, limit, 0,
                    "No recipes are available in the catalog yet.",
                    aiProposals, aiMessage
            );
        }

        Set<UUID> recipeFilter = request.recipeIds() != null ? new HashSet<>(request.recipeIds()) : null;
        List<MatchingEngine.MatchResult> matches = matchingEngine.matchRecipes(
                usable, recipes, minMatch, limit, recipeFilter
        );

        if (matches.isEmpty()) {
            String message = aiProposals.isEmpty()
                    ? "No recipes match your pantry at the requested threshold. Try lowering the minimum match percentage or add more ingredients."
                    : "No catalog recipes matched, but the AI Chef proposed creative ideas below.";
            return new RecipeDtos.SuggestionResponse(
                    List.of(), 0, 1, limit, 0, message, aiProposals, aiMessage
            );
        }

        List<RecipeDtos.SuggestionItemResponse> suggestions = matches.stream()
                .map(this::toSuggestionItem)
                .toList();
        saveCatalogHistory(userId, matches);
        return new RecipeDtos.SuggestionResponse(
                suggestions,
                suggestions.size(),
                1,
                limit,
                1,
                null,
                aiProposals,
                aiMessage
        );
    }

    @Transactional(readOnly = true)
    public RecipeDtos.SuggestionHistoryListResponse history(
            UUID userId,
            int page,
            int pageSize,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        List<SuggestionHistory> all = historyRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("userId"), userId),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        List<SuggestionHistory> filtered = all.stream()
                .filter(entry -> fromDate == null || !entry.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().isBefore(fromDate))
                .filter(entry -> toDate == null || !entry.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().isAfter(toDate))
                .toList();
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        int fromIndex = Math.min((safePage - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());
        List<RecipeDtos.SuggestionHistoryResponse> items = filtered.subList(fromIndex, toIndex).stream()
                .map(DtoMapper::toHistoryResponse)
                .toList();
        return new RecipeDtos.SuggestionHistoryListResponse(
                items, filtered.size(), safePage, safePageSize, DtoMapper.pages(filtered.size(), safePageSize)
        );
    }

    @Transactional
    public RecipeDtos.RecipeIngestResponse ingest(RecipeDtos.RecipeIngestRequest request) {
        if (recipeRepository.existsByName(request.name())) {
            Recipe existing = recipeRepository.findAll().stream()
                    .filter(recipe -> recipe.getName().equalsIgnoreCase(request.name()))
                    .findFirst()
                    .orElseThrow();
            return new RecipeDtos.RecipeIngestResponse(existing.getId(), existing.getName(), false);
        }
        Recipe recipe = new Recipe();
        recipe.setName(request.name());
        recipe.setCategory(request.category() != null ? request.category() : "Other");
        recipe.setPreparation(request.preparation());
        recipe.setSourceType("crawler");
        recipe.setSourceUrl(request.sourceUrl());
        if (request.ingredients() != null) {
            for (RecipeDtos.RecipeIngredientInput input : request.ingredients()) {
                RecipeIngredient line = new RecipeIngredient();
                line.setRecipe(recipe);
                line.setProductName(input.productName());
                line.setQuantity(input.quantity());
                line.setUnit(input.unit() != null ? input.unit() : "piece");
                recipe.getIngredients().add(line);
            }
        }
        recipe = recipeRepository.save(recipe);
        // New recipe changes the catalog set -> drop the cached snapshot once this tx commits.
        recipeCatalogCache.evictAfterCommit();
        aiServiceClient.reindexRecipe(recipe.getId());
        return new RecipeDtos.RecipeIngestResponse(recipe.getId(), recipe.getName(), true);
    }

    private RecipeDtos.DailyPicksResponse popularFallback(
            List<Recipe> recipes,
            Map<UUID, Integer> favoriteCounts,
            UUID userId,
            LocalDate today,
            int limit,
            int pantryTotal
    ) {
        List<Recipe> ranked = recipes.stream()
                .sorted(Comparator.comparingDouble((Recipe r) -> -popularity(r, favoriteCounts))
                        .thenComparing(r -> r.getName().toLowerCase()))
                .toList();
        Set<UUID> favoriteIds = userId != null ? favoriteRepository.findRecipeIdsByUserId(userId) : Set.of();
        List<RecipeDtos.DailyPickResponse> items = new ArrayList<>();
        for (Recipe recipe : ranked.stream().limit(limit).toList()) {
            items.add(new RecipeDtos.DailyPickResponse(
                    DtoMapper.toRecipeResponse(
                            recipe,
                            favoriteCounts.getOrDefault(recipe.getId(), 0),
                            favoriteIds.contains(recipe.getId())),
                    0.0,
                    false,
                    Math.round(popularity(recipe, favoriteCounts) * 10.0) / 10.0,
                    List.of("popular"),
                    List.of()
            ));
        }
        String message = pantryTotal == 0
                ? "Add ingredients to your pantry to unlock personalised picks. Here are some popular recipes to get you started."
                : "Nothing matched your pantry today — here are popular recipes instead.";
        return new RecipeDtos.DailyPicksResponse(items, today, message);
    }

    private RecipeDtos.SuggestionItemResponse toSuggestionItem(MatchingEngine.MatchResult match) {
        return new RecipeDtos.SuggestionItemResponse(
                match.recipeId(),
                match.name(),
                match.preparation(),
                match.matchPercent(),
                match.ingredients(),
                match.usedIngredients(),
                match.missingIngredients(),
                match.canPrepare()
        );
    }

    private void saveCatalogHistory(UUID userId, List<MatchingEngine.MatchResult> matches) {
        for (MatchingEngine.MatchResult match : matches) {
            SuggestionHistory entry = new SuggestionHistory();
            entry.setUserId(userId);
            entry.setRecipeId(match.recipeId());
            entry.setRecipeName(match.name());
            entry.setMatchPercent(match.matchPercent());
            entry.setUsedIngredients(match.usedIngredients());
            entry.setMissingIngredients(match.missingIngredients());
            entry.setSource("catalog");
            historyRepository.save(entry);
        }
    }

    private void saveAiHistory(UUID userId, List<RecipeDtos.AiRecipeProposal> proposals) {
        for (RecipeDtos.AiRecipeProposal proposal : proposals) {
            SuggestionHistory entry = new SuggestionHistory();
            entry.setUserId(userId);
            entry.setRecipeName(proposal.name());
            entry.setSource("ai");
            entry.setAgentLabel(proposal.agentLabel());
            entry.setPreparation(proposal.preparation());
            entry.setUsedIngredients(List.of());
            entry.setMissingIngredients(List.of());
            if (proposal.ingredients() != null) {
                entry.setIngredients(proposal.ingredients().stream()
                        .map(item -> {
                            SuggestionHistory.HistoryIngredientEntry mapped = new SuggestionHistory.HistoryIngredientEntry();
                            mapped.setProductName(item.productName());
                            mapped.setQuantity(item.quantity());
                            mapped.setUnit(item.unit());
                            return mapped;
                        })
                        .toList());
            }
            historyRepository.save(entry);
        }
    }

    /**
     * Builds the per-user favorite view (counts + which recipes the user favorited) WITHOUT mutating
     * the recipe entities — the catalog snapshot is shared across users and must stay immutable.
     * Apply it at DTO-mapping time via {@link #mapRecipe(Recipe, FavoriteView)}.
     */
    private FavoriteView favoriteView(List<Recipe> recipes, UUID userId) {
        if (recipes.isEmpty()) {
            return new FavoriteView(Map.of(), Set.of());
        }
        List<UUID> ids = recipes.stream().map(Recipe::getId).toList();
        Map<UUID, Integer> counts = favoriteCounts(ids);
        Set<UUID> favoriteIds = userId != null ? favoriteRepository.findRecipeIdsByUserId(userId) : Set.of();
        return new FavoriteView(counts, favoriteIds);
    }

    private RecipeDtos.RecipeResponse mapRecipe(Recipe recipe, FavoriteView fav) {
        return DtoMapper.toRecipeResponse(
                recipe,
                fav.counts().getOrDefault(recipe.getId(), 0),
                fav.favoriteIds().contains(recipe.getId()));
    }

    private record FavoriteView(Map<UUID, Integer> counts, Set<UUID> favoriteIds) {
    }

    private double popularity(Recipe recipe, Map<UUID, Integer> favoriteCounts) {
        return recipe.getViewCount() + 3.0 * favoriteCounts.getOrDefault(recipe.getId(), 0);
    }

    private Map<UUID, Integer> favoriteCounts() {
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : favoriteRepository.countGroupedByRecipeId()) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    private Map<UUID, Integer> favoriteCounts(List<UUID> recipeIds) {
        if (recipeIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : favoriteRepository.countGroupedByRecipeIdIn(recipeIds)) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }
}
