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

    /** Short lifetime for internal service-to-service tokens — just long enough to make the call. */
    private static final long SERVICE_TOKEN_TTL_SECONDS = 60;

    /** Marker claim identifying a service (not user) token. */
    public static final String SERVICE_CLAIM = "svc";

    public JwtService(RsaKeyProvider rsaKeyProvider, AppProperties appProperties) {
        this.rsaKeyProvider = rsaKeyProvider;
        this.appProperties = appProperties;
    }

    /**
     * Mints a short-lived RS256 token proving the caller is core, for internal calls to other
     * services (e.g. the gateway revocation push). The receiver verifies it via core's JWKS — no
     * shared secret is transmitted, and a captured token is useless after {@value #SERVICE_TOKEN_TTL_SECONDS}s.
     */
    public String createServiceToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("babaai-core")
                .claim(SERVICE_CLAIM, true)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(SERVICE_TOKEN_TTL_SECONDS)))
                .signWith(rsaKeyProvider.getPrivateKey())
                .compact();
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
