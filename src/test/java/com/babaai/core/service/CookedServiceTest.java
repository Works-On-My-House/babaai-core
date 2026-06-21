package com.babaai.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.Ingredient;
import com.babaai.core.domain.Recipe;
import com.babaai.core.domain.RecipeIngredient;
import com.babaai.core.dto.CookedDtos;
import com.babaai.core.repository.CookedEventRepository;
import com.babaai.core.repository.IngredientRepository;
import com.babaai.core.repository.RecipeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.data.jpa.domain.Specification;
import tools.jackson.databind.json.JsonMapper;

/** Unit coverage of the pantry-decrement math (unit-aware, never negative, incompatible-unit handling). */
class CookedServiceTest {

    private RecipeRepository recipeRepository;
    private IngredientRepository ingredientRepository;
    private CookedEventRepository cookedEventRepository;
    private CookedService service;

    @BeforeEach
    void setUp() {
        recipeRepository = mock(RecipeRepository.class);
        ingredientRepository = mock(IngredientRepository.class);
        cookedEventRepository = mock(CookedEventRepository.class);

        AppProperties appProperties = new AppProperties();
        JsonConfigCache cache = new JsonConfigCache(new DefaultResourceLoader(), JsonMapper.builder().build());
        JsonConfigService config = new JsonConfigService(cache, appProperties);
        UnitConversion unitConversion = new UnitConversion(config);

        service = new CookedService(recipeRepository, ingredientRepository, cookedEventRepository, unitConversion);
    }

    private Recipe recipe(RecipeIngredient... lines) {
        Recipe recipe = new Recipe();
        recipe.setId(UUID.randomUUID());
        recipe.setName("Test Recipe");
        recipe.setVerified(true);
        for (RecipeIngredient line : lines) {
            line.setRecipe(recipe);
            recipe.getIngredients().add(line);
        }
        return recipe;
    }

    private RecipeIngredient line(String product, double quantity, String unit) {
        RecipeIngredient line = new RecipeIngredient();
        line.setProductName(product);
        line.setQuantity(quantity);
        line.setUnit(unit);
        return line;
    }

    private Ingredient pantryItem(String name, double quantity, String unit) {
        Ingredient item = new Ingredient();
        item.setId(UUID.randomUUID());
        item.setName(name);
        item.setQuantity(quantity);
        item.setUnit(unit);
        return item;
    }

    @Test
    void decrementsMatchingPantryAndReportsIncompatibleUnits() {
        Recipe recipe = recipe(
                line("Tomato", 4, "pcs"),
                line("Pasta", 200, "g"),
                line("Milk", 200, "ml")); // volume vs a count pantry item -> incompatible
        when(recipeRepository.findWithIngredientsByIdAndVerifiedTrue(recipe.getId()))
                .thenReturn(Optional.of(recipe));

        Ingredient tomato = pantryItem("Tomato", 5, "pcs");
        Ingredient pasta = pantryItem("Pasta", 300, "g");
        Ingredient milk = pantryItem("Milk", 3, "pcs");
        when(ingredientRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(tomato, pasta, milk));

        CookedDtos.CookedResponse response = service.markCooked(UUID.randomUUID(), recipe.getId(), null);

        assertThat(response.consumed()).extracting(CookedDtos.ConsumedLineResponse::productName)
                .containsExactlyInAnyOrder("Tomato", "Pasta");
        assertThat(response.unmatchedIngredients()).containsExactly("Milk");
        assertThat(tomato.getQuantity()).isEqualTo(1.0);   // 5 - 4
        assertThat(pasta.getQuantity()).isEqualTo(100.0);  // 300 - 200
        assertThat(milk.getQuantity()).isEqualTo(3.0);     // untouched (incompatible unit)
        verify(cookedEventRepository).save(any());
    }

    @Test
    void neverDrivesQuantityNegativeAndRemovesDepletedItem() {
        Recipe recipe = recipe(line("Tomato", 4, "pcs")); // needs more than the pantry holds
        when(recipeRepository.findWithIngredientsByIdAndVerifiedTrue(recipe.getId()))
                .thenReturn(Optional.of(recipe));
        Ingredient tomato = pantryItem("Tomato", 2, "pcs");
        when(ingredientRepository.findAll(any(Specification.class))).thenReturn(List.of(tomato));

        CookedDtos.CookedResponse response = service.markCooked(UUID.randomUUID(), recipe.getId(), null);

        // Consumed what was available (2), the depleted row is deleted (never negative).
        assertThat(response.consumed()).hasSize(1);
        assertThat(response.consumed().get(0).quantity()).isEqualTo(2.0);
        verify(ingredientRepository).delete(tomato);
    }
}
