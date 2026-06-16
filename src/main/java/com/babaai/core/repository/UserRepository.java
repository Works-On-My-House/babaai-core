package com.babaai.core.repository;

import com.babaai.core.domain.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "permissionOverrides", "permissionOverrides.permission"})
    Optional<User> findWithRolesAndPermissionsById(UUID id);

    // Direct update: presence is high-frequency, so skip @Version/@PreUpdate (no optimistic-lock churn).
    @Modifying
    @Query("update User u set u.lastSeenAt = :now where u.id = :id")
    void touchLastSeen(@Param("id") UUID id, @Param("now") Instant now);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmailOrUsername(String email, String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
