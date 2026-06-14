package com.babaai.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(
            UUID id,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("updated_at") Instant updatedAt,
            Integer version,
            @JsonProperty("user_id") UUID userId,
            String title,
            String message,
            String type,
            @JsonProperty("read") boolean read
    ) {
    }

    public record NotificationListResponse(
            List<NotificationResponse> items,
            int total,
            @JsonProperty("unread_count") long unreadCount
    ) {
    }

    public record MarkReadResponse(
            UUID id,
            @JsonProperty("read") boolean read,
            @JsonProperty("updated_at") Instant updatedAt
    ) {
    }
}
