package com.babaai.core.api;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.User;
import com.babaai.core.dto.RecipeDtos;
import com.babaai.core.security.SecurityUtils;
import com.babaai.core.service.DailySuggestionService;
import com.babaai.core.service.RecipeService;
import com.babaai.core.service.RescueService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final DailySuggestionService dailySuggestionService;
    private final RescueService rescueService;
    private final AppProperties appProperties;

    public RecipeController(
            RecipeService recipeService,
            DailySuggestionService dailySuggestionService,
            RescueService rescueService,
            AppProperties appProperties
    ) {
        this.recipeService = recipeService;
        this.dailySuggestionService = dailySuggestionService;
        this.rescueService = rescueService;
        this.appProperties = appProperties;
    }

    @GetMapping
    public RecipeDtos.RecipeListResponse list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category
    ) {
        User user = SecurityUtils.optionalUser();
        int size = pageSize != null ? pageSize : appProperties.getDefaults().getPageSize();
        return recipeService.list(page, size, search, category, user != null ? user.getId() : null);
    }

    @GetMapping("/categories")
    public RecipeDtos.RecipeCategoriesResponse categories() {
        return recipeService.listCategories();
    }

    @GetMapping("/featured")
    public RecipeDtos.RecipeResponse featured() {
        User user = SecurityUtils.optionalUser();
        return recipeService.featured(user != null ? user.getId() : null, LocalDate.now());
    }

    @GetMapping("/daily")
    public RecipeDtos.DailyPicksResponse daily(@RequestParam(defaultValue = "4") int limit) {
        return recipeService.dailyPicks(SecurityUtils.requireUser().getId(), LocalDate.now(), limit);
    }

    /**
     * Personalized "Today for you" set (869dr0a4d): pantry + expiry + taste-profile aware, persisted
     * per user/day. Generated lazily on first read of the day if the scheduled job hasn't run yet.
     */
    @GetMapping("/today")
    public RecipeDtos.DailyPicksResponse today(@RequestParam(required = false) Integer limit) {
        int size = limit != null ? limit : appProperties.getSuggestions().getDailyLimit();
        return dailySuggestionService.today(SecurityUtils.requireUser().getId(), LocalDate.now(), size);
    }

    /**
     * Rescue Mode (869dtvycn): recipes that use pantry items expiring soon, ranked by how many they
     * rescue. The waste-reduction "use it up" surface.
     */
    @GetMapping("/rescue")
    public RecipeDtos.RescueResponse rescue(@RequestParam(required = false) Integer limit) {
        int size = limit != null ? limit : appProperties.getRescue().getLimit();
        return rescueService.rescue(SecurityUtils.requireUser().getId(), LocalDate.now(), size);
    }

    @GetMapping("/favorites")
    public RecipeDtos.FavoriteListResponse favorites() {
        return recipeService.listFavorites(SecurityUtils.requireUser().getId());
    }

    @PostMapping("/suggestions")
    public RecipeDtos.SuggestionResponse suggestions(@Valid @RequestBody RecipeDtos.SuggestionRequest request) {
        return recipeService.generateSuggestions(SecurityUtils.requireUser().getId(), request);
    }

    @GetMapping("/history")
    public RecipeDtos.SuggestionHistoryListResponse history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(name = "from_date", required = false) LocalDate fromDate,
            @RequestParam(name = "to_date", required = false) LocalDate toDate
    ) {
        int size = pageSize != null ? pageSize : appProperties.getDefaults().getPageSize();
        return recipeService.history(SecurityUtils.requireUser().getId(), page, size, fromDate, toDate);
    }

    @PostMapping("/{recipeId}/favorite")
    public RecipeDtos.FavoriteResponse addFavorite(@PathVariable UUID recipeId) {
        return recipeService.addFavorite(SecurityUtils.requireUser().getId(), recipeId);
    }

    @DeleteMapping("/{recipeId}/favorite")
    public RecipeDtos.FavoriteResponse removeFavorite(@PathVariable UUID recipeId) {
        return recipeService.removeFavorite(SecurityUtils.requireUser().getId(), recipeId);
    }

    @PostMapping("/{recipeId}/view")
    public RecipeDtos.RecipeResponse view(@PathVariable UUID recipeId) {
        User user = SecurityUtils.optionalUser();
        return recipeService.recordView(recipeId, user != null ? user.getId() : null);
    }

    @GetMapping("/{recipeId}")
    public RecipeDtos.RecipeResponse get(@PathVariable UUID recipeId) {
        User user = SecurityUtils.optionalUser();
        return recipeService.get(recipeId, user != null ? user.getId() : null);
    }
}
