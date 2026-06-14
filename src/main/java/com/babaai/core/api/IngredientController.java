package com.babaai.core.api;

import com.babaai.core.config.AppProperties;
import com.babaai.core.dto.IngredientDtos;
import com.babaai.core.security.SecurityUtils;
import com.babaai.core.service.IngredientService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;
    private final AppProperties appProperties;

    public IngredientController(IngredientService ingredientService, AppProperties appProperties) {
        this.ingredientService = ingredientService;
        this.appProperties = appProperties;
    }

    @GetMapping
    public IngredientDtos.IngredientListResponse list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "asc") String sortOrder
    ) {
        int size = pageSize != null ? pageSize : appProperties.getDefaults().getPageSize();
        return ingredientService.list(
                SecurityUtils.requireUser().getId(),
                page,
                size,
                search,
                category,
                sortBy,
                sortOrder
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientDtos.IngredientResponse create(@Valid @RequestBody IngredientDtos.IngredientCreateRequest request) {
        return ingredientService.create(SecurityUtils.requireUser().getId(), request);
    }

    @GetMapping("/{ingredientId}")
    public IngredientDtos.IngredientResponse get(@PathVariable UUID ingredientId) {
        return ingredientService.get(SecurityUtils.requireUser().getId(), ingredientId);
    }

    @PutMapping("/{ingredientId}")
    public IngredientDtos.IngredientResponse update(
            @PathVariable UUID ingredientId,
            @Valid @RequestBody IngredientDtos.IngredientUpdateRequest request
    ) {
        return ingredientService.update(SecurityUtils.requireUser().getId(), ingredientId, request);
    }

    @DeleteMapping("/{ingredientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID ingredientId) {
        ingredientService.delete(SecurityUtils.requireUser().getId(), ingredientId);
    }
}
