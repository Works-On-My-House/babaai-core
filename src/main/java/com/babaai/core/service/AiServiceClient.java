package com.babaai.core.service;

import com.babaai.core.dto.RecipeDtos;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final WebClient aiWebClient;

    public AiServiceClient(@Qualifier("aiWebClient") WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    // @JsonProperty pins the wire name: these WebClients serialize with a default camelCase mapper,
    // and ai expects snake_case (pantry_names) — without this the list is dropped and ai gets [].
    public record ProposalRequest(@JsonProperty("pantry_names") List<String> pantryNames, int limit) {
    }

    public record ProposalResponse(List<RecipeDtos.AiRecipeProposal> proposals, String error) {
    }

    /** Proposals plus an optional user-facing message explaining why there are none. */
    public record AiProposals(List<RecipeDtos.AiRecipeProposal> proposals, String message) {
        public static AiProposals empty() {
            return new AiProposals(List.of(), null);
        }
    }

    /**
     * Best-effort AI proposals. Never throws — a failure returns an empty list plus a message so the
     * caller can surface it to the user, without blocking the (core-computed) catalog suggestions.
     */
    public AiProposals fetchProposals(List<String> pantryNames, int limit) {
        log.info("Requesting AI proposals: pantryItems={}, limit={}", pantryNames.size(), limit);
        if (pantryNames.isEmpty()) {
            return AiProposals.empty();
        }
        try {
            ProposalResponse response = aiWebClient.post()
                    .uri("/api/v1/ai/proposals")
                    .bodyValue(new ProposalRequest(pantryNames, limit))
                    .retrieve()
                    .bodyToMono(ProposalResponse.class)
                    .block();
            if (response == null) {
                log.warn("AI proposal request returned no body");
                return new AiProposals(List.of(), "AI suggestions are temporarily unavailable.");
            }
            List<RecipeDtos.AiRecipeProposal> proposals =
                    response.proposals() != null ? response.proposals() : List.of();
            log.info("AI proposals received: count={}, error={}", proposals.size(), response.error());
            return new AiProposals(proposals, response.error());
        } catch (Exception ex) {
            log.warn("AI proposal request failed: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
            return new AiProposals(List.of(), "AI suggestions are temporarily unavailable.");
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
