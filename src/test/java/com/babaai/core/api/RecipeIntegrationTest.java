package com.babaai.core.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class RecipeIntegrationTest extends AbstractIntegrationTest {

    @Test
    void listSeededRecipesWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    void listRecipeCategories() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray());
    }

    @Test
    void authenticatedSuggestionsWithoutAi() throws Exception {
        String accessToken = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "recipes");

        mockMvc.perform(
                        post("/api/v1/ingredients")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Egg",
                                          "category": "Dairy",
                                          "quantity": 6,
                                          "unit": "pcs"
                                        }
                                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/v1/recipes/suggestions")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "min_match_percent": 10,
                                          "limit": 5,
                                          "include_ai": false
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions").isArray());
    }
}
