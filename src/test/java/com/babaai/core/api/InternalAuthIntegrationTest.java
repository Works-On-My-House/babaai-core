package com.babaai.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.domain.User;
import com.babaai.core.repository.UserRepository;
import com.babaai.core.security.JwtService;
import com.babaai.core.security.RsaKeyProvider;
import com.babaai.core.service.GatewayRevocationClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies the revocation trigger: the internal endpoint bumps permissions_version and pushes it to
 * the gateway after the transaction commits (ClickUp 869dqmbfp). The gateway client is mocked — the
 * cross-service hop is covered by the gateway's TokenRevocationIntegrationTest.
 */
class InternalAuthIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private GatewayRevocationClient gatewayRevocationClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RsaKeyProvider rsaKeyProvider;

    private UUID persistUser() {
        User user = new User();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        user.setEmail("revoke-" + suffix + "@example.com");
        user.setUsername("revoke_" + suffix);
        user.setHashedPassword("x");
        return userRepository.save(user).getId();
    }

    @Test
    void revokeBumpsVersionAndNotifiesGatewayAfterCommit() throws Exception {
        UUID userId = persistUser();

        mockMvc.perform(post("/api/v1/internal/auth/users/{id}/revoke", userId)
                        .header("X-Service-Token", "test-ai-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions_version").value(1));

        // After-commit hook pushed the new pv to the gateway.
        verify(gatewayRevocationClient).revoke(userId, 1);
        // And it was persisted.
        assertThat(userRepository.findById(userId).orElseThrow().getPermissionsVersion()).isEqualTo(1);
    }

    @Test
    void revokeRequiresServiceToken() throws Exception {
        mockMvc.perform(post("/api/v1/internal/auth/users/{id}/revoke", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void serviceTokenIsSignedAndMarked() {
        // The token the gateway will verify via core's JWKS: valid signature + svc marker.
        Claims claims = Jwts.parser()
                .verifyWith(rsaKeyProvider.getPublicKey())
                .build()
                .parseSignedClaims(jwtService.createServiceToken())
                .getPayload();
        assertThat(claims.getSubject()).isEqualTo("babaai-core");
        assertThat(claims.get("svc", Boolean.class)).isTrue();
    }
}
