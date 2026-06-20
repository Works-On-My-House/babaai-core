package com.babaai.core.api;

import com.babaai.core.dto.RecipeDtos;
import com.babaai.core.service.RecipeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/recipes")
public class InternalRecipeController {

    private final RecipeService recipeService;

    public InternalRecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @PostMapping("/ingest")
    public RecipeDtos.RecipeIngestResponse ingest(@Valid @RequestBody RecipeDtos.RecipeIngestRequest request) {
        return recipeService.ingest(request);
    }

    /**
     * Bulk file-import of curated recipes (869dqrre0). Imported recipes are created verified with
     * nutrition computed. Behind the internal service-token (mirrors /ingest); an admin-permission
     * variant can front this once admin auth lands (869dqjtpc).
     */
    @PostMapping("/import")
    public RecipeDtos.RecipeImportResponse importRecipes(@Valid @RequestBody RecipeDtos.RecipeImportRequest request) {
        return recipeService.importRecipes(request);
    }
}
