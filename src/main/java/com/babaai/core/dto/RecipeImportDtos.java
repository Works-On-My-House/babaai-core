package com.babaai.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RecipeImportDtos {

    private RecipeImportDtos() {
    }

    public record RecipeImportResponse(
            UUID id,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt,
            Integer version,
            @JsonProperty("original_filename") String originalFilename,
            @JsonProperty("content_type") String contentType,
            @JsonProperty("size_bytes") long sizeBytes,
            String status,
            @JsonProperty("review_note") String reviewNote,
            @JsonProperty("decided_at") Instant decidedAt
    ) {
    }

    public record RecipeImportListResponse(List<RecipeImportResponse> items, int total) {
    }
}
