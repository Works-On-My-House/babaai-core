package com.babaai.core.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.dto.RecipeDtos;
import com.babaai.core.security.ServiceTokenFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Recipe enrichment (869dqrre0): a curated import becomes verified + listed with computed per-serving
 * nutrition; a crawled recipe stays unverified and is hidden from every public read path.
 */
class RecipeEnrichmentIntegrationTest extends AbstractIntegrationTest {

    private static final String SERVICE_TOKEN = "test-ai-token";

    @Test
    void importedRecipeIsVerifiedListedWithNutrition() throws Exception {
        String body =
                """
                {
                  "recipes": [
                    {
                      "name": "Imported Cheese Omelette ZZZ",
                      "category": "Breakfast",
                      "preparation": "Whisk eggs, cook in butter, fold in cheese.",
                      "servings": 2,
                      "ingredients": [
                        { "product_name": "Eggs", "quantity": 4, "unit": "pcs" },
                        { "product_name": "Cheese", "quantity": 50, "unit": "g" }
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/internal/recipes/import")
                        .header(ServiceTokenFilter.SERVICE_TOKEN_HEADER, SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.skipped").value(0));

        mockMvc.perform(get("/api/v1/recipes").param("search", "Imported Cheese Omelette ZZZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Imported Cheese Omelette ZZZ"))
                .andExpect(jsonPath("$.items[0].nutrition.complete").value(true))
                .andExpect(jsonPath("$.items[0].nutrition.servings").value(2))
                .andExpect(jsonPath("$.items[0].nutrition.calories").isNumber());
    }

    @Test
    void crawledRecipeIsUnverifiedAndHidden() throws Exception {
        String body =
                """
                {
                  "name": "Hidden Crawler Dish QWE",
                  "category": "Other",
                  "preparation": "Mix and serve.",
                  "ingredients": [ { "product_name": "Tomato", "quantity": 2, "unit": "pcs" } ]
                }
                """;

        String response = mockMvc.perform(post("/api/v1/internal/recipes/ingest")
                        .header(ServiceTokenFilter.SERVICE_TOKEN_HEADER, SERVICE_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        RecipeDtos.RecipeIngestResponse ingest =
                objectMapper.readValue(response, RecipeDtos.RecipeIngestResponse.class);

        // Hidden from the public listing...
        mockMvc.perform(get("/api/v1/recipes").param("search", "Hidden Crawler Dish QWE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        // ...and 404 on the detail path.
        mockMvc.perform(get("/api/v1/recipes/{id}", ingest.recipeId()))
                .andExpect(status().isNotFound());
    }
}
