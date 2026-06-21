package com.babaai.core.repository;

import com.babaai.core.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Idempotency guard for the nightly digest: has this user already had a notification of this
    // type since the given instant (start of today)?
    boolean existsByUserIdAndTypeAndCreatedAtGreaterThanEqual(UUID userId, String type, Instant createdAt);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndReadFalse(UUID userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
    void markAllRead(UUID userId);
}
