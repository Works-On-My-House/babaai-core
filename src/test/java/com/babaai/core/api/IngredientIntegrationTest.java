package com.babaai.core.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

class IngredientIntegrationTest extends AbstractIntegrationTest {

    @Test
    void crudIngredientLifecycle() throws Exception {
        String accessToken = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "pantry");

        MvcResult createResult = mockMvc.perform(
                        post("/api/v1/ingredients")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Tomato",
                                          "category": "Vegetables",
                                          "quantity": 2,
                                          "unit": "pcs"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tomato"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String ingredientId = created.get("id").asString();

        mockMvc.perform(
                        get("/api/v1/ingredients")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("search", "Tomato"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Tomato"));

        mockMvc.perform(
                        get("/api/v1/ingredients/{id}", ingredientId)
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2));

        mockMvc.perform(
                        put("/api/v1/ingredients/{id}", ingredientId)
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "name": "Cherry Tomato",
                                          "category": "Vegetables",
                                          "quantity": 5,
                                          "unit": "pcs"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cherry Tomato"))
                .andExpect(jsonPath("$.quantity").value(5));

        mockMvc.perform(
                        delete("/api/v1/ingredients/{id}", ingredientId)
                                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/ingredients")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("search", "Cherry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }
}
