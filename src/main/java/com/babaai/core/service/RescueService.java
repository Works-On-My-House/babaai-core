package com.babaai.core.service;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.Ingredient;
import com.babaai.core.domain.Recipe;
import com.babaai.core.dto.RecipeDtos;
import com.babaai.core.repository.NotificationRepository;
import com.babaai.core.repository.RecipeFavoriteRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rescue Mode (869dtvycn): surfaces catalog recipes that use soon-to-expire pantry items so users
 * cook them before they spoil, and writes a nightly "expiry digest" notification. Deterministic and
 * free-tier — reuses {@link MatchingEngine} (its {@code usedIngredients}) against the verified catalog.
 */
@Service
public class RescueService {

    static final String DIGEST_TYPE = "expiry_digest";

    private final RecipeCatalogCache catalogCache;
    private final IngredientService ingredientService;
    private final MatchingEngine matchingEngine;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final RecipeFavoriteRepository favoriteRepository;
    private final AppProperties appProperties;

    public RescueService(
            RecipeCatalogCache catalogCache,
            IngredientService ingredientService,
            MatchingEngine matchingEngine,
            NotificationService notificationService,
            NotificationRepository notificationRepository,
            RecipeFavoriteRepository favoriteRepository,
            AppProperties appProperties
    ) {
        this.catalogCache = catalogCache;
        this.ingredientService = ingredientService;
        this.matchingEngine = matchingEngine;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.favoriteRepository = favoriteRepository;
        this.appProperties = appProperties;
    }

    /** Recipes ranked by how many soon-to-expire pantry items they consume. */
    @Transactional(readOnly = true)
    public RecipeDtos.RescueResponse rescue(UUID userId, LocalDate today, int limit) {
        AppProperties.Rescue config = appProperties.getRescue();
        List<Ingredient> pantry = ingredientService.listUsable(userId);

        // normalized name -> original display name, for pantry items expiring within the lead window.
        LocalDate cutoff = today.plusDays(config.getLeadDays());
        Map<String, String> expiringByNorm = new LinkedHashMap<>();
        for (Ingredient item : pantry) {
            LocalDate expiry = item.getExpirationDate();
            if (expiry != null && !expiry.isAfter(cutoff)) {
                expiringByNorm.putIfAbsent(normalize(item.getName()), item.getName());
            }
        }
        List<String> expiring = new ArrayList<>(expiringByNorm.values());
        if (expiring.isEmpty()) {
            return new RecipeDtos.RescueResponse(List.of(), List.of(), today,
                    "Nothing in your pantry is expiring soon — nice!");
        }

        List<Recipe> catalog = catalogCache.snapshot();
        Map<UUID, Recipe> byId = new HashMap<>();
        catalog.forEach(recipe -> byId.put(recipe.getId(), recipe));
        Set<UUID> favoriteIds = favoriteRepository.findRecipeIdsByUserId(userId);
        Map<UUID, Integer> favoriteCounts = favoriteCounts();

        List<Scored> scored = new ArrayList<>();
        for (MatchingEngine.MatchResult match : matchingEngine.matchRecipes(pantry, catalog, 0, catalog.size(), null)) {
            List<String> rescued = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (String used : match.usedIngredients()) {
                String norm = normalize(used);
                if (expiringByNorm.containsKey(norm) && seen.add(norm)) {
                    rescued.add(expiringByNorm.get(norm));
                }
            }
            Recipe recipe = byId.get(match.recipeId());
            if (rescued.isEmpty() || recipe == null) {
                continue;
            }
            scored.add(new Scored(recipe, match, rescued));
        }

        scored.sort(Comparator
                .comparingInt((Scored s) -> s.rescued().size()).reversed()
                .thenComparing(Comparator.comparingDouble((Scored s) -> s.match().matchPercent()).reversed())
                .thenComparing((Scored s) -> s.match().canPrepare() ? 0 : 1)
                .thenComparing((Scored s) -> s.recipe().getName().toLowerCase(Locale.ROOT)));

        List<RecipeDtos.RescueItemResponse> items = scored.stream()
                .limit(Math.max(1, limit))
                .map(s -> new RecipeDtos.RescueItemResponse(
                        DtoMapper.toRecipeResponse(
                                s.recipe(),
                                favoriteCounts.getOrDefault(s.recipe().getId(), 0),
                                favoriteIds.contains(s.recipe().getId())),
                        s.match().matchPercent(),
                        s.match().canPrepare(),
                        s.rescued(),
                        s.match().missingIngredients()))
                .toList();

        String message = items.isEmpty()
                ? "You have items expiring soon, but no catalog recipes use them yet."
                : null;
        return new RecipeDtos.RescueResponse(items, expiring, today, message);
    }

    /**
     * Writes at most one {@code expiry_digest} notification per user per day. Returns true when a
     * digest was created (i.e. the user has expiring items and hadn't been notified today).
     */
    @Transactional
    public boolean generateDigest(UUID userId, LocalDate today) {
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        if (notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(userId, DIGEST_TYPE, startOfDay)) {
            return false;
        }
        RecipeDtos.RescueResponse rescue = rescue(userId, today, appProperties.getRescue().getLimit());
        if (rescue.expiringIngredients().isEmpty()) {
            return false;
        }
        int count = rescue.expiringIngredients().size();
        String title = "Use it up — " + count + (count == 1 ? " item expiring soon" : " items expiring soon");
        String expiringList = String.join(", ", rescue.expiringIngredients());
        String message;
        if (rescue.items().isEmpty()) {
            message = expiringList + " — expiring within " + appProperties.getRescue().getLeadDays() + " days.";
        } else {
            String topRecipes = rescue.items().stream()
                    .limit(3)
                    .map(item -> item.recipe().name())
                    .collect(Collectors.joining(", "));
            message = expiringList + " expiring soon. Cook these to use them up: " + topRecipes + ".";
        }
        notificationService.create(userId, title, message, DIGEST_TYPE);
        return true;
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

    private record Scored(Recipe recipe, MatchingEngine.MatchResult match, List<String> rescued) {
    }
}
