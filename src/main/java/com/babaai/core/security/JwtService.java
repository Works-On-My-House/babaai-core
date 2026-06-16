package com.babaai.core.security;

import com.babaai.core.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final RsaKeyProvider rsaKeyProvider;
    private final AppProperties appProperties;

    public JwtService(RsaKeyProvider rsaKeyProvider, AppProperties appProperties) {
        this.rsaKeyProvider = rsaKeyProvider;
        this.appProperties = appProperties;
    }

    /**
     * Mints a stateless access token. The {@code permissions} and {@code pv}
     * (permissions_version) claims let downstream services (ai, gateway) authorize without calling
     * core; {@code pv} supports token-freshness checks.
     */
    public String createAccessToken(UUID userId, Collection<String> permissions, int permissionsVersion) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(appProperties.getJwt().getExpireMinutes() * 60L);
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("permissions", permissions)
                .claim("pv", permissionsVersion)
                .signWith(rsaKeyProvider.getPrivateKey())
                .compact();
    }

    public Optional<UUID> parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(rsaKeyProvider.getPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(subject));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }
}
