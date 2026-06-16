package com.babaai.core.service;

import com.babaai.core.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracks user presence by stamping {@code last_seen_at} on activity. Throttled so a burst of
 * requests results in at most one write per {@link #THROTTLE}; derive "online" from
 * {@code lastSeenAt > now - window} rather than storing a boolean.
 */
@Service
public class PresenceService {

    private static final Duration THROTTLE = Duration.ofSeconds(60);

    private final UserRepository userRepository;

    public PresenceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Whether {@code lastSeenAt} is old enough that it's worth writing a new timestamp. */
    public boolean isStale(Instant lastSeenAt, Instant now) {
        return lastSeenAt == null || lastSeenAt.isBefore(now.minus(THROTTLE));
    }

    @Transactional
    public void touch(UUID userId, Instant now) {
        userRepository.touchLastSeen(userId, now);
    }
}
