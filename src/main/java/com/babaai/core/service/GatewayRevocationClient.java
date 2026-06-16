package com.babaai.core.service;

import com.babaai.core.security.JwtService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Pushes token-revocation events to the gateway so a user's currently-valid access token (which
 * still carries stale {@code permissions}) is rejected near-instantly instead of lingering until
 * its ≤15-min TTL expires. The gateway holds a short-lived denylist keyed by {@code permissions_version}
 * (pv); the refreshed token carries the new pv and passes. See ClickUp 869dqmbfp.
 *
 * <p>Authenticates with a freshly-minted short-lived RS256 service token (verified by the gateway via
 * core's JWKS) — no shared secret on the wire. Fire-and-forget and best-effort: if the gateway is
 * unreachable the call is logged and dropped, and the system falls back to the TTL bound.
 */
@Service
public class GatewayRevocationClient {

    /** Header carrying the signed core service token (distinct from the static X-Service-Token path). */
    public static final String SERVICE_AUTH_HEADER = "X-Service-Auth";

    private static final Logger log = LoggerFactory.getLogger(GatewayRevocationClient.class);

    private final WebClient gatewayWebClient;
    private final JwtService jwtService;

    public GatewayRevocationClient(
            @Qualifier("gatewayWebClient") WebClient gatewayWebClient, JwtService jwtService) {
        this.gatewayWebClient = gatewayWebClient;
        this.jwtService = jwtService;
    }

    // @JsonProperty pins the wire name (default camelCase mapper); the gateway expects user_id.
    public record RevocationRequest(@JsonProperty("user_id") UUID userId, int pv) {
    }

    /** Notifies the gateway that tokens for {@code userId} with {@code pv < permissionsVersion} are stale. */
    public void revoke(UUID userId, int permissionsVersion) {
        try {
            gatewayWebClient.post()
                    .uri("/internal/auth/revocations")
                    .header(SERVICE_AUTH_HEADER, jwtService.createServiceToken())
                    .bodyValue(new RevocationRequest(userId, permissionsVersion))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            // Best-effort: gateway falls back to the access-token TTL bound. Don't fail the caller.
            log.warn("Gateway revocation push failed for user {}: {}", userId, ex.getMessage());
        }
    }
}
