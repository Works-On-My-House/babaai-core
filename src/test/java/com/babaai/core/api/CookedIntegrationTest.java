package com.babaai.core.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.support.AuthTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/** "Cooked it" (869dtvyct): cooking a recipe deducts matching pantry items and logs what was consumed. */
class CookedIntegrationTest extends AbstractIntegrationTest {

    private void addIngredient(String token, String name, String unit, double quantity) throws Exception {
        mockMvc.perform(post("/api/v1/ingredients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "%s", "quantity": %s, "unit": "%s" }
                                """.formatted(name, quantity, unit)))
                .andExpect(status().isCreated());
    }

    private String recipeIdByName(String name) throws Exception {
        String body = mockMvc.perform(get("/api/v1/recipes").param("search", name))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("items").get(0).get("id").asText();
    }

    @Test
    void cookingDecrementsPantryAndLogsConsumed() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "cook");
        addIngredient(token, "Tomato", "pcs", 5);   // recipe uses 4
        addIngredient(token, "Pasta", "g", 300);     // recipe uses 200
        String recipeId = recipeIdByName("Tomato Pasta");

        mockMvc.perform(post("/api/v1/recipes/{id}/cooked", recipeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipe_name").value("Tomato Pasta"))
                .andExpect(jsonPath("$.consumed[*].product_name", hasItems("Pasta", "Tomato")))
                .andExpect(jsonPath("$.unmatched_ingredients", hasItem("Garlic")));

        mockMvc.perform(get("/api/v1/ingredients").param("search", "Pasta")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items[0].quantity").value(100.0));
        mockMvc.perform(get("/api/v1/ingredients").param("search", "Tomato")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.items[0].quantity").value(1.0));
    }

    @Test
    void cookingNeverDrivesQuantityNegative() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "cook-neg");
        addIngredient(token, "Tomato", "pcs", 2);   // recipe needs 4 -> consumed to zero, removed
        String recipeId = recipeIdByName("Tomato Pasta");

        mockMvc.perform(post("/api/v1/recipes/{id}/cooked", recipeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/ingredients").param("search", "Tomato")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void cookingRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{id}/cooked", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
