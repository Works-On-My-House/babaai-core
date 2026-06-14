package com.babaai.core.service;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.Ingredient;
import com.babaai.core.domain.IngredientCategory;
import com.babaai.core.dto.ConfigDtos;
import com.babaai.core.dto.IngredientDtos;
import com.babaai.core.exception.AppException;
import com.babaai.core.exception.NotFoundException;
import com.babaai.core.repository.IngredientCategoryRepository;
import com.babaai.core.repository.IngredientRepository;
import com.babaai.core.repository.IngredientSpecifications;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientCategoryRepository categoryRepository;
    private final AppProperties appProperties;

    public IngredientService(
            IngredientRepository ingredientRepository,
            IngredientCategoryRepository categoryRepository,
            AppProperties appProperties
    ) {
        this.ingredientRepository = ingredientRepository;
        this.categoryRepository = categoryRepository;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public IngredientDtos.IngredientListResponse list(
            UUID userId,
            int page,
            int pageSize,
            String search,
            String category,
            String sortBy,
            String sortOrder
    ) {
        Specification<Ingredient> spec = IngredientSpecifications.filter(userId, search, category);

        Sort sort = buildSort(sortBy, sortOrder);
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Page<Ingredient> result = ingredientRepository.findAll(
                spec,
                PageRequest.of(safePage - 1, safePageSize, sort)
        );
        List<IngredientDtos.IngredientResponse> items = result.getContent().stream()
                .map(ingredient -> DtoMapper.toIngredientResponse(ingredient, ingredient.getCategoryName()))
                .toList();
        return new IngredientDtos.IngredientListResponse(
                items,
                (int) result.getTotalElements(),
                safePage,
                safePageSize,
                DtoMapper.pages((int) result.getTotalElements(), safePageSize)
        );
    }

    @Transactional
    public IngredientDtos.IngredientResponse create(UUID userId, IngredientDtos.IngredientCreateRequest request) {
        IngredientCategory category = resolveCategory(request.category(), request.name());
        Ingredient ingredient = new Ingredient();
        ingredient.setUserId(userId);
        ingredient.setCategoryId(category.getId());
        apply(ingredient, request.name(), request.quantity(), request.unit(), request.expirationDate(), request.notes());
        ingredient = ingredientRepository.save(ingredient);
        ingredient.setCategoryRef(category);
        return DtoMapper.toIngredientResponse(ingredient, category.getName());
    }

    @Transactional(readOnly = true)
    public IngredientDtos.IngredientResponse get(UUID userId, UUID ingredientId) {
        Ingredient ingredient = findOwned(userId, ingredientId);
        return DtoMapper.toIngredientResponse(ingredient, ingredient.getCategoryName());
    }

    @Transactional
    public IngredientDtos.IngredientResponse update(
            UUID userId,
            UUID ingredientId,
            IngredientDtos.IngredientUpdateRequest request
    ) {
        Ingredient ingredient = findOwned(userId, ingredientId);
        IngredientCategory category = resolveCategory(request.category(), request.name());
        ingredient.setCategoryId(category.getId());
        ingredient.setCategoryRef(category);
        apply(ingredient, request.name(), request.quantity(), request.unit(), request.expirationDate(), request.notes());
        return DtoMapper.toIngredientResponse(ingredientRepository.save(ingredient), category.getName());
    }

    @Transactional
    public void delete(UUID userId, UUID ingredientId) {
        Ingredient ingredient = findOwned(userId, ingredientId);
        ingredientRepository.delete(ingredient);
    }

    public ConfigDtos.InferCategoryResponse inferCategory(String name) {
        return new ConfigDtos.InferCategoryResponse(name, inferCategoryName(name));
    }

    public String inferCategoryName(String name) {
        String normalized = name.strip().toLowerCase(Locale.ROOT);
        for (IngredientCategory category : categoryRepository.findAllByOrderByNameAsc()) {
            for (String keyword : category.getKeywords()) {
                if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return category.getName();
                }
            }
        }
        return categoryRepository.findFirstByIsDefaultTrue()
                .map(IngredientCategory::getName)
                .orElse(appProperties.getDefaults().getIngredientCategory());
    }

    public List<Ingredient> listUsable(UUID userId) {
        return ingredientRepository.findAll(
                IngredientSpecifications.forUser(userId),
                Sort.by("name")
        ).stream()
                .filter(this::isUsable)
                .toList();
    }

    public List<Ingredient> listByIds(UUID userId, List<UUID> ids) {
        List<Ingredient> items = ingredientRepository.findByUserIdAndIdIn(userId, ids);
        if (items.size() != ids.stream().distinct().count()) {
            throw new AppException("One or more selected ingredients were not found in your pantry");
        }
        return items;
    }

    private Ingredient findOwned(UUID userId, UUID ingredientId) {
        return ingredientRepository.findByIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new NotFoundException("Ingredient not found"));
    }

    private IngredientCategory resolveCategory(String categoryName, String ingredientName) {
        final String resolved;
        if (categoryName != null && !categoryName.isBlank()) {
            resolved = categoryName;
        } else if (ingredientName != null && !ingredientName.isBlank()) {
            resolved = inferCategoryName(ingredientName);
        } else {
            resolved = appProperties.getDefaults().getIngredientCategory();
        }
        return categoryRepository.findByName(resolved)
                .orElseThrow(() -> new AppException("Unknown ingredient category: " + resolved));
    }

    private void apply(
            Ingredient ingredient,
            String name,
            double quantity,
            String unit,
            java.time.LocalDate expirationDate,
            String notes
    ) {
        ingredient.setName(name.strip());
        ingredient.setQuantity(quantity);
        ingredient.setUnit(unit.strip());
        ingredient.setExpirationDate(expirationDate);
        ingredient.setNotes(notes);
    }

    private boolean isUsable(Ingredient ingredient) {
        return ingredient.getExpirationDate() == null
                || !ingredient.getExpirationDate().isBefore(java.time.LocalDate.now());
    }

    private Sort buildSort(String sortBy, String sortOrder) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property = switch (sortBy == null ? "" : sortBy) {
            case "quantity" -> "quantity";
            case "expiration_date" -> "expirationDate";
            case "created_at" -> "createdAt";
            default -> "name";
        };
        return Sort.by(direction, property);
    }
}
