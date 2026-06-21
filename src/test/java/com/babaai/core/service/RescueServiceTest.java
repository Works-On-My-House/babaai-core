package com.babaai.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.Ingredient;
import com.babaai.core.domain.Recipe;
import com.babaai.core.domain.RecipeIngredient;
import com.babaai.core.dto.RecipeDtos;
import com.babaai.core.repository.NotificationRepository;
import com.babaai.core.repository.RecipeFavoriteRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.json.JsonMapper;

/** Unit coverage of rescue ranking + nightly-digest idempotency (real MatchingEngine, mocked deps). */
class RescueServiceTest {

    private final UUID userId = UUID.randomUUID();
    private RecipeCatalogCache catalogCache;
    private IngredientService ingredientService;
    private NotificationService notificationService;
    private NotificationRepository notificationRepository;
    private RecipeFavoriteRepository favoriteRepository;
    private AppProperties appProperties;
    private RescueService service;

    @BeforeEach
    void setUp() {
        catalogCache = mock(RecipeCatalogCache.class);
        ingredientService = mock(IngredientService.class);
        notificationService = mock(NotificationService.class);
        notificationRepository = mock(NotificationRepository.class);
        favoriteRepository = mock(RecipeFavoriteRepository.class);
        when(favoriteRepository.findRecipeIdsByUserId(any())).thenReturn(Set.of());
        when(favoriteRepository.countGroupedByRecipeId()).thenReturn(List.of());

        appProperties = new AppProperties();
        appProperties.getRescue().setLeadDays(3);
        appProperties.getRescue().setLimit(5);

        JsonConfigCache cache = new JsonConfigCache(new DefaultResourceLoader(), JsonMapper.builder().build());
        JsonConfigService config = new JsonConfigService(cache, appProperties);
        MatchingEngine matchingEngine = new MatchingEngine(new UnitConversion(config));

        service = new RescueService(catalogCache, ingredientService, matchingEngine,
                notificationService, notificationRepository, favoriteRepository, appProperties);
    }

    private Ingredient pantryItem(String name, double quantity, String unit, LocalDate expiration) {
        Ingredient item = new Ingredient();
        item.setUserId(userId);
        item.setName(name);
        item.setQuantity(quantity);
        item.setUnit(unit);
        item.setExpirationDate(expiration);
        return item;
    }

    private Recipe recipe(String name, String... productNames) {
        Recipe recipe = new Recipe();
        recipe.setId(UUID.randomUUID());
        recipe.setName(name);
        recipe.setVerified(true);
        for (String product : productNames) {
            RecipeIngredient line = new RecipeIngredient();
            line.setRecipe(recipe);
            line.setProductName(product);
            line.setQuantity(1);
            line.setUnit("pcs");
            recipe.getIngredients().add(line);
        }
        return recipe;
    }

    @Test
    void ranksByCountOfExpiringItemsRescued() {
        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(1);
        LocalDate later = today.plusDays(30);

        when(ingredientService.listUsable(userId)).thenReturn(List.of(
                pantryItem("Tomato", 4, "pcs", soon),
                pantryItem("Spinach", 200, "g", soon),
                pantryItem("Rice", 500, "g", later)));

        Recipe both = recipe("Rescue Both", "Tomato", "Spinach", "Rice"); // rescues Tomato + Spinach
        Recipe one = recipe("Rescue One", "Tomato", "Onion");             // rescues Tomato
        Recipe none = recipe("Rescue None", "Rice", "Onion");            // rescues nothing expiring
        when(catalogCache.snapshot()).thenReturn(List.of(one, none, both));

        RecipeDtos.RescueResponse response = service.rescue(userId, today, 5);

        assertThat(response.expiringIngredients()).containsExactlyInAnyOrder("Tomato", "Spinach");
        assertThat(response.items()).hasSize(2); // "Rescue None" excluded
        assertThat(response.items().get(0).recipe().name()).isEqualTo("Rescue Both");
        assertThat(response.items().get(0).rescuedIngredients()).containsExactlyInAnyOrder("Tomato", "Spinach");
        assertThat(response.items().get(1).rescuedIngredients()).containsExactly("Tomato");
    }

    @Test
    void emptyWhenNothingExpiring() {
        LocalDate today = LocalDate.now();
        when(ingredientService.listUsable(userId))
                .thenReturn(List.of(pantryItem("Rice", 500, "g", today.plusDays(30))));

        RecipeDtos.RescueResponse response = service.rescue(userId, today, 5);

        assertThat(response.items()).isEmpty();
        assertThat(response.expiringIngredients()).isEmpty();
        assertThat(response.message()).isNotNull();
    }

    @Test
    void digestIsIdempotentPerDay() {
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                eq(userId), eq(RescueService.DIGEST_TYPE), any(Instant.class))).thenReturn(true);

        boolean created = service.generateDigest(userId, LocalDate.now());

        assertThat(created).isFalse();
        verify(notificationService, never()).create(any(), anyString(), anyString(), anyString());
    }

    @Test
    void digestCreatedWhenItemsExpiringAndNotYetSent() {
        LocalDate today = LocalDate.now();
        when(notificationRepository.existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(
                any(), anyString(), any(Instant.class))).thenReturn(false);
        when(ingredientService.listUsable(userId))
                .thenReturn(List.of(pantryItem("Tomato", 4, "pcs", today.plusDays(1))));
        when(catalogCache.snapshot()).thenReturn(List.of(recipe("Tomato Soup", "Tomato", "Onion")));

        boolean created = service.generateDigest(userId, today);

        assertThat(created).isTrue();
        verify(notificationService).create(eq(userId), anyString(), anyString(), eq(RescueService.DIGEST_TYPE));
    }
}
