package com.babaai.core.service;

import com.babaai.core.dto.RecipeDtos;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final WebClient aiWebClient;

    public AiServiceClient(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    public record ProposalRequest(List<String> pantryNames, int limit) {
    }

    public record ProposalResponse(List<RecipeDtos.AiRecipeProposal> proposals) {
    }

    public List<RecipeDtos.AiRecipeProposal> fetchProposals(List<String> pantryNames, int limit) {
        if (pantryNames.isEmpty()) {
            return List.of();
        }
        try {
            ProposalResponse response = aiWebClient.post()
                    .uri("/api/v1/ai/proposals")
                    .bodyValue(new ProposalRequest(pantryNames, limit))
                    .retrieve()
                    .bodyToMono(ProposalResponse.class)
                    .block();
            return response != null && response.proposals() != null ? response.proposals() : List.of();
        } catch (Exception ex) {
            log.warn("AI proposal request failed: {}", ex.getMessage());
            return List.of();
        }
    }

    public void reindexRecipe(UUID recipeId) {
        try {
            aiWebClient.post()
                    .uri("/api/v1/ai/reindex")
                    .bodyValue(new RecipeDtos.ReindexRequest(recipeId))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            log.warn("AI reindex failed for recipe {}: {}", recipeId, ex.getMessage());
        }
    }
}
