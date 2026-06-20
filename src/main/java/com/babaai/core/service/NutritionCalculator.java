package com.babaai.core.service;

import com.babaai.core.domain.RecipeIngredient;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Computes a recipe's TOTAL nutrition from its ingredient lines (quantity x unit) using a curated
 * per-100g reference ({@code ingredient_nutrition.json}) plus {@link UnitConversion}.
 *
 * <p>Deterministic and write-time only (seed / import / future verify) so reads stay cheap and
 * cache-friendly. Per-serving values are derived as {@code total / servings} at DTO-mapping time.
 * Lines that cannot be matched to the reference, or cannot be converted to grams (e.g. a count unit
 * with no {@code grams_per_piece}), are skipped and flip {@code complete} to false — partial totals
 * are kept rather than failing the recipe (the strategy's "approximate but useful" stance).
 */
@Component
public class NutritionCalculator {

    private final JsonConfigService jsonConfigService;
    private final UnitConversion unitConversion;

    public NutritionCalculator(JsonConfigService jsonConfigService, UnitConversion unitConversion) {
        this.jsonConfigService = jsonConfigService;
        this.unitConversion = unitConversion;
    }

    /** A single ingredient line to be weighted. */
    public record Line(String productName, double quantity, String unit) {
    }

    /** Recipe total macros. {@code complete} is true only when every line was matched and weighed. */
    public record Nutrition(Double calories, Double proteinG, Double carbsG, Double fatG, boolean complete) {
        public static Nutrition empty() {
            return new Nutrition(null, null, null, null, false);
        }
    }

    public Nutrition computeFromIngredients(List<RecipeIngredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return Nutrition.empty();
        }
        return compute(ingredients.stream()
                .map(i -> new Line(i.getProductName(), i.getQuantity(), i.getUnit()))
                .toList());
    }

    public Nutrition compute(List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            return Nutrition.empty();
        }
        Map<String, Map<String, Object>> reference = jsonConfigService.nutritionReference();
        Map<String, String> aliases = jsonConfigService.nutritionAliases();

        double calories = 0;
        double protein = 0;
        double carbs = 0;
        double fat = 0;
        boolean complete = true;
        int matched = 0;
        for (Line line : lines) {
            Map<String, Object> entry = lookup(reference, aliases, line.productName());
            Double grams = entry == null ? null : toGrams(line.quantity(), line.unit(), entry);
            if (entry == null || grams == null) {
                complete = false;
                continue;
            }
            matched++;
            Map<String, Object> per100g = asMap(entry.get("per_100g"));
            double factor = grams / 100.0;
            calories += factor * number(per100g.get("calories"));
            protein += factor * number(per100g.get("protein_g"));
            carbs += factor * number(per100g.get("carbs_g"));
            fat += factor * number(per100g.get("fat_g"));
        }
        if (matched == 0) {
            // Nothing matched the reference -> no usable totals (null), not a misleading zero.
            return Nutrition.empty();
        }
        return new Nutrition(round(calories), round(protein), round(carbs), round(fat), complete);
    }

    private Map<String, Object> lookup(
            Map<String, Map<String, Object>> reference,
            Map<String, String> aliases,
            String productName
    ) {
        if (productName == null) {
            return null;
        }
        String key = productName.strip().toLowerCase();
        Map<String, Object> direct = resolve(reference, aliases, key);
        if (direct != null) {
            return direct;
        }
        // Plural -> singular fallback ("tomatoes" -> "tomato").
        if (key.endsWith("s") && key.length() > 1) {
            return resolve(reference, aliases, key.substring(0, key.length() - 1));
        }
        return null;
    }

    private Map<String, Object> resolve(
            Map<String, Map<String, Object>> reference,
            Map<String, String> aliases,
            String key
    ) {
        if (reference.containsKey(key)) {
            return reference.get(key);
        }
        String aliased = aliases.get(key);
        if (aliased != null && reference.containsKey(aliased)) {
            return reference.get(aliased);
        }
        return null;
    }

    /** Convert a line's quantity+unit to grams using mass/volume(+density)/count(+grams_per_piece). */
    private Double toGrams(double quantity, String unit, Map<String, Object> entry) {
        UnitConversion.BaseQuantity base = unitConversion.toBaseQuantity(quantity, unit);
        if (base == null) {
            return null;
        }
        return switch (base.family()) {
            case "mass" -> base.amount();
            case "volume" -> base.amount() * density(entry);
            case "count" -> {
                Double gramsPerPiece = optionalNumber(entry.get("grams_per_piece"));
                yield gramsPerPiece == null ? null : base.amount() * gramsPerPiece;
            }
            default -> null;
        };
    }

    private double density(Map<String, Object> entry) {
        Double density = optionalNumber(entry.get("density_g_per_ml"));
        // Volume line without a curated density: fall back to water-like 1.0 g/ml rather than dropping
        // the line. Curated densities exist for oils/dairy/syrups where it matters most.
        return density == null ? 1.0 : density;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private double number(Object value) {
        Double parsed = optionalNumber(value);
        return parsed == null ? 0.0 : parsed;
    }

    private Double optionalNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
