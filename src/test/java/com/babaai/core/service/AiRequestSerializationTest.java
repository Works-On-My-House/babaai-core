package com.babaai.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.babaai.core.dto.RecipeDtos;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * The dedicated WebClients serialize with a default (camelCase) Jackson mapper, so outbound request
 * DTOs must pin their wire names with {@code @JsonProperty} to match ai's snake_case contract.
 * Regression guard for the "pantry_names dropped → ai returns no suggestions" bug.
 */
class AiRequestSerializationTest {

    // No SNAKE_CASE strategy configured — mirrors the WebClient's mapper.
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void proposalRequestUsesSnakeCasePantryNames() {
        String json = mapper.writeValueAsString(
                new AiServiceClient.ProposalRequest(List.of("egg", "milk"), 3));
        assertThat(json).contains("\"pantry_names\"").doesNotContain("pantryNames");
    }

    @Test
    void reindexRequestUsesSnakeCaseRecipeId() {
        String json = mapper.writeValueAsString(new RecipeDtos.ReindexRequest(UUID.randomUUID()));
        assertThat(json).contains("\"recipe_id\"").doesNotContain("recipeId");
    }

    @Test
    void revocationRequestUsesSnakeCaseUserId() {
        String json = mapper.writeValueAsString(
                new GatewayRevocationClient.RevocationRequest(UUID.randomUUID(), 2));
        assertThat(json).contains("\"user_id\"").doesNotContain("userId");
    }
}
