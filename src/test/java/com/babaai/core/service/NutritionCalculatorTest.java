package com.babaai.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.babaai.core.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.json.JsonMapper;

/**
 * Exercises the nutrition engine against the real curated reference + unit-conversion config on the
 * classpath (no Spring context, no DB).
 */
class NutritionCalculatorTest {

    private NutritionCalculator newCalculator() {
        AppProperties props = new AppProperties();
        JsonConfigCache cache = new JsonConfigCache(new DefaultResourceLoader(), JsonMapper.builder().build());
        JsonConfigService config = new JsonConfigService(cache, props);
        UnitConversion units = new UnitConversion(config);
        return new NutritionCalculator(config, units);
    }

    @Test
    void massUnitsConvertDirectlyToGrams() {
        // 200 g pasta at 371 kcal / 100 g => 742 kcal.
        NutritionCalculator.Nutrition n =
                newCalculator().compute(List.of(new NutritionCalculator.Line("Pasta", 200, "g")));
        assertThat(n.complete()).isTrue();
        assertThat(n.calories()).isCloseTo(742.0, within(1.0));
    }

    @Test
    void countUnitsUseGramsPerPiece() {
        // 2 eggs (50 g each = 100 g) at 143 kcal / 100 g => 143 kcal.
        NutritionCalculator.Nutrition n =
                newCalculator().compute(List.of(new NutritionCalculator.Line("Eggs", 2, "pcs")));
        assertThat(n.complete()).isTrue();
        assertThat(n.calories()).isCloseTo(143.0, within(1.0));
    }

    @Test
    void volumeUnitsUseDensity() {
        // 1 tbsp olive oil = 15 ml * 0.91 g/ml = 13.65 g at 884 kcal / 100 g => ~120.7 kcal.
        NutritionCalculator.Nutrition n =
                newCalculator().compute(List.of(new NutritionCalculator.Line("Olive Oil", 1, "tbsp")));
        assertThat(n.complete()).isTrue();
        assertThat(n.calories()).isCloseTo(120.7, within(2.0));
    }

    @Test
    void unmatchedIngredientMarksIncompleteButKeepsPartialTotals() {
        NutritionCalculator.Nutrition n = newCalculator().compute(List.of(
                new NutritionCalculator.Line("Pasta", 100, "g"),
                new NutritionCalculator.Line("Moondust", 50, "g")));
        assertThat(n.complete()).isFalse();
        assertThat(n.calories()).isCloseTo(371.0, within(1.0)); // pasta only
    }

    @Test
    void fullyUnmatchedReturnsNullTotals() {
        NutritionCalculator.Nutrition n =
                newCalculator().compute(List.of(new NutritionCalculator.Line("Moondust", 50, "g")));
        assertThat(n.complete()).isFalse();
        assertThat(n.calories()).isNull();
    }
}
