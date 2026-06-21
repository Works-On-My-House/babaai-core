package com.babaai.core.service;

import com.babaai.core.domain.CookedEvent;
import com.babaai.core.domain.Ingredient;
import com.babaai.core.domain.Recipe;
import com.babaai.core.domain.RecipeIngredient;
import com.babaai.core.dto.CookedDtos;
import com.babaai.core.exception.NotFoundException;
import com.babaai.core.repository.CookedEventRepository;
import com.babaai.core.repository.IngredientRepository;
import com.babaai.core.repository.IngredientSpecifications;
import com.babaai.core.repository.RecipeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
 * "Cooked it" consumption tracking (869dtvyct): deducts a cooked recipe's ingredients from the user's
 * pantry (unit-aware, soonest-expiring first, never below zero) and records a {@link CookedEvent} with
 * the consumed snapshot. Lines with no pantry match or an incompatible unit are skipped and reported.
 */
@Service
public class CookedService {

    private static final double EPSILON = 1e-6;

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final CookedEventRepository cookedEventRepository;
    private final UnitConversion unitConversion;

    public CookedService(
            RecipeRepository recipeRepository,
            IngredientRepository ingredientRepository,
            CookedEventRepository cookedEventRepository,
            UnitConversion unitConversion
    ) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.cookedEventRepository = cookedEventRepository;
        this.unitConversion = unitConversion;
    }

    @Transactional
    public CookedDtos.CookedResponse markCooked(UUID userId, UUID recipeId, Integer servings) {
        Recipe recipe = recipeRepository.findWithIngredientsByIdAndVerifiedTrue(recipeId)
                .orElseThrow(() -> new NotFoundException("Recipe not found"));
        double scale = scaleFor(recipe, servings);

        // Pantry grouped by normalized name; consume soonest-expiring first within each group.
        Map<String, List<Ingredient>> byName = new HashMap<>();
        for (Ingredient item : ingredientRepository.findAll(IngredientSpecifications.forUser(userId))) {
            byName.computeIfAbsent(normalize(item.getName()), key -> new ArrayList<>()).add(item);
        }
        byName.values().forEach(list -> list.sort(
                Comparator.comparing(Ingredient::getExpirationDate, Comparator.nullsLast(Comparator.naturalOrder()))));

        Set<UUID> removed = new HashSet<>();
        List<CookedEvent.ConsumedLine> consumed = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();

        for (RecipeIngredient line : recipe.getIngredients()) {
            UnitConversion.BaseQuantity required =
                    unitConversion.toBaseQuantity(line.getQuantity() * scale, line.getUnit());
            List<Ingredient> matches = byName.get(normalize(line.getProductName()));
            if (matches == null || matches.isEmpty() || required == null) {
                unmatched.add(line.getProductName());
                continue;
            }

            double remaining = required.amount();
            double consumedBase = 0.0;
            for (Ingredient item : matches) {
                if (remaining <= EPSILON) {
                    break;
                }
                if (removed.contains(item.getId())) {
                    continue;
                }
                UnitConversion.BaseQuantity itemBase = unitConversion.toBaseQuantity(item.getQuantity(), item.getUnit());
                if (itemBase == null || !itemBase.family().equals(required.family())) {
                    continue; // incompatible unit family (e.g. recipe in pcs, pantry in grams)
                }
                double take = Math.min(remaining, itemBase.amount());
                if (take <= EPSILON) {
                    continue;
                }
                remaining -= take;
                consumedBase += take;
                double newBase = itemBase.amount() - take;
                if (newBase <= EPSILON) {
                    ingredientRepository.delete(item);
                    removed.add(item.getId());
                } else {
                    Double newQuantity = unitConversion.fromBaseAmount(newBase, item.getUnit());
                    item.setQuantity(newQuantity != null ? round(newQuantity) : 0.0);
                    ingredientRepository.save(item);
                }
            }

            if (consumedBase <= EPSILON) {
                unmatched.add(line.getProductName()); // name matched but no compatible-unit pantry item
                continue;
            }
            Double consumedInUnit = unitConversion.fromBaseAmount(consumedBase, line.getUnit());
            CookedEvent.ConsumedLine entry = new CookedEvent.ConsumedLine();
            entry.setProductName(line.getProductName());
            entry.setQuantity(consumedInUnit != null ? round(consumedInUnit) : null);
            entry.setUnit(line.getUnit());
            consumed.add(entry);
        }

        CookedEvent event = new CookedEvent();
        event.setUserId(userId);
        event.setRecipeId(recipe.getId());
        event.setRecipeName(recipe.getName());
        event.setCookedAt(Instant.now());
        event.setConsumed(consumed);
        cookedEventRepository.save(event);

        List<CookedDtos.ConsumedLineResponse> consumedDto = consumed.stream()
                .map(c -> new CookedDtos.ConsumedLineResponse(c.getProductName(), c.getQuantity(), c.getUnit()))
                .toList();
        return new CookedDtos.CookedResponse(recipe.getId(), recipe.getName(), consumedDto, unmatched);
    }

    private double scaleFor(Recipe recipe, Integer servings) {
        if (servings != null && servings > 0 && recipe.getServings() != null && recipe.getServings() > 0) {
            return (double) servings / recipe.getServings();
        }
        return 1.0;
    }

    private String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
