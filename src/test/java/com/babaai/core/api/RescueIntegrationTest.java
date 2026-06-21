package com.babaai.core.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.support.AuthTestSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Rescue Mode (869dtvycn): /recipes/rescue surfaces recipes that use soon-to-expire pantry items. */
class RescueIntegrationTest extends AbstractIntegrationTest {

    private void addIngredient(String token, String name, String unit, double quantity, String expiration)
            throws Exception {
        mockMvc.perform(post("/api/v1/ingredients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "%s", "quantity": %s, "unit": "%s", "expiration_date": "%s" }
                                """.formatted(name, quantity, unit, expiration)))
                .andExpect(status().isCreated());
    }

    @Test
    void rescueRanksRecipesUsingExpiringItems() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "rescue");
        String soon = LocalDate.now().plusDays(1).toString();
        String later = LocalDate.now().plusDays(30).toString();
        addIngredient(token, "Tomato", "pcs", 4, soon);   // expiring
        addIngredient(token, "Rice", "g", 500, later);     // not expiring

        mockMvc.perform(get("/api/v1/recipes/rescue")
                        .param("limit", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiring_ingredients", hasItem("Tomato")))
                .andExpect(jsonPath("$.expiring_ingredients", not(hasItem("Rice"))))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].rescued_ingredients", hasItem("Tomato")));
    }

    @Test
    void rescueEmptyWhenNothingExpiring() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "rescue-empty");
        addIngredient(token, "Rice", "g", 500, LocalDate.now().plusDays(30).toString());

        mockMvc.perform(get("/api/v1/recipes/rescue").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.expiring_ingredients.length()").value(0))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void rescueRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/rescue")).andExpect(status().isUnauthorized());
    }
}
