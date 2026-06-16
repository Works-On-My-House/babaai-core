package com.babaai.core.service;

import com.babaai.core.config.AppProperties;
import com.babaai.core.domain.RefreshToken;
import com.babaai.core.exception.AppException;
import com.babaai.core.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rotating refresh tokens with reuse detection. The opaque token is high-entropy random; only its
 * SHA-256 hash is stored. Each rotation issues a new token in the same family and marks the old one
 * used; presenting an already-used/revoked token revokes the whole family (theft response).
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32; // 256-bit opaque token

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository repository;
    private final AppProperties appProperties;

    public RefreshTokenService(RefreshTokenRepository repository, AppProperties appProperties) {
        this.repository = repository;
        this.appProperties = appProperties;
    }

    public record Rotation(UUID userId, String refreshToken) {}

    @Transactional
    public String issue(UUID userId) {
        String raw = generateToken();
        persist(userId, UUID.randomUUID(), raw);
        return raw;
    }

    // noRollbackFor: when reuse is detected we revokeFamily THEN throw — that revocation must commit.
    @Transactional(noRollbackFor = AppException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken token = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (token.isRevoked() || token.isUsed()) {
            repository.revokeFamily(token.getFamilyId());
            throw new AppException(
                    "Refresh token reuse detected; sessions revoked. Please sign in again.",
                    HttpStatus.UNAUTHORIZED);
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }

        token.setUsed(true);
        repository.save(token);

        String raw = generateToken();
        persist(token.getUserId(), token.getFamilyId(), raw); // rotate within the same family
        return new Rotation(token.getUserId(), raw);
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId()));
    }

    private void persist(UUID userId, UUID familyId, String rawToken) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setFamilyId(familyId);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(Duration.ofDays(appProperties.getJwt().getRefreshExpireDays())));
        repository.save(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
