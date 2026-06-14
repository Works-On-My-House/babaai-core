package com.babaai.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class IngredientDtos {

    private IngredientDtos() {
    }

    public record IngredientCreateRequest(
            @NotBlank @Size(max = 255) String name,
            String category,
            @Min(0) double quantity,
            @NotBlank @Size(max = 50) String unit,
            @JsonProperty("expiration_date") LocalDate expirationDate,
            String notes
    ) {
    }

    public record IngredientUpdateRequest(
            @NotBlank @Size(max = 255) String name,
            String category,
            @Min(0) double quantity,
            @NotBlank @Size(max = 50) String unit,
            @JsonProperty("expiration_date") LocalDate expirationDate,
            String notes
    ) {
    }

    public record IngredientResponse(
            UUID id,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt,
            Integer version,
            String name,
            @JsonProperty("category_id") UUID categoryId,
            String category,
            double quantity,
            String unit,
            @JsonProperty("expiration_date") LocalDate expirationDate,
            String notes,
            @JsonProperty("user_id") UUID userId
    ) {
    }

    public record IngredientListResponse(
            List<IngredientResponse> items,
            int total,
            int page,
            @JsonProperty("page_size") int pageSize,
            int pages
    ) {
    }
}
