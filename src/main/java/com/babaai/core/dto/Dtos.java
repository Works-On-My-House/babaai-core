package com.babaai.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class Dtos {

    private Dtos() {
    }

    public record BaseEntityDto(
            UUID id,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt,
            Integer version
    ) {
    }

    public record UserCreateRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 3, max = 100) String username,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {
    }

    public record UserResponse(
            UUID id,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt,
            Integer version,
            String email,
            String username
    ) {
    }

    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType
    ) {
        public TokenResponse(String accessToken) {
            this(accessToken, "bearer");
        }
    }

    public record HealthResponse(String status) {
        public static HealthResponse ok() {
            return new HealthResponse("ok");
        }
    }
}
