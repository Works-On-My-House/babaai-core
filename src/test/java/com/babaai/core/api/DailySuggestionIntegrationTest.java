package com.babaai.core.api;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.support.AuthTestSupport;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Taste profile + daily personalized, pantry/expiry-aware suggestions (869dr0a4d). */
class DailySuggestionIntegrationTest extends AbstractIntegrationTest {

    @Test
    void preferencesRoundTripAndNormalize() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "prefs");

        mockMvc.perform(put("/api/v1/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "preferred_ingredients": ["Tomato", "Tomato"],
                                  "disliked_ingredients": ["Olives"],
                                  "preferred_categories": ["Pasta"],
                                  "dietary_tags": ["Vegetarian"],
                                  "allergens": ["Shellfish"]
                                }
                                """))
                .andExpect(status().isOk())
                // dedup + dietary tags/allergens lower-cased for config matching
                .andExpect(jsonPath("$.preferred_ingredients.length()").value(1))
                .andExpect(jsonPath("$.preferred_categories[0]").value("Pasta"))
                .andExpect(jsonPath("$.dietary_tags[0]").value("vegetarian"))
                .andExpect(jsonPath("$.allergens[0]").value("shellfish"));

        mockMvc.perform(get("/api/v1/preferences").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferred_ingredients[0]").value("Tomato"))
                .andExpect(jsonPath("$.disliked_ingredients[0]").value("Olives"));
    }

    @Test
    void todayPrioritisesExpiringPantryItems() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "expiry");
        String soon = LocalDate.now().plusDays(1).toString();

        // A pantry item expiring tomorrow that many seeded recipes use -> those recipes get the
        // waste-reduction "expiring" boost and dominate the personalized ranking.
        mockMvc.perform(post("/api/v1/ingredients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name": "Tomato",
                                  "quantity": 4,
                                  "unit": "pcs",
                                  "expiration_date": "%s"
                                }
                                """.formatted(soon)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/recipes/today")
                        .param("limit", "6")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated_for").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].reasons", hasItem("expiring")))
                .andExpect(jsonPath("$.items[0].match_percent").isNumber())
                .andExpect(jsonPath("$.items[0].recipe.nutrition").exists());
    }

    @Test
    void todayRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/today")).andExpect(status().isUnauthorized());
    }
}
