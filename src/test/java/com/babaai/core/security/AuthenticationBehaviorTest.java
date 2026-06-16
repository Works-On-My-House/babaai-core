package com.babaai.core.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pins down what happens to an endpoint with NO permission annotation:
 * unauthenticated -> 401, any authenticated user -> 200 (no permission required).
 * The endpoint is under /api/v1/test/** so it falls through to .anyRequest().authenticated().
 */
class AuthenticationBehaviorTest extends AbstractIntegrationTest {

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/test/authenticated-only"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anyAuthenticatedUserCanAccessUnannotatedEndpoint() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "noperm");
        mockMvc.perform(get("/api/v1/test/authenticated-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        TestOpenController testOpenController() {
            return new TestOpenController();
        }
    }

    @RestController
    static class TestOpenController {

        // No @HasPermission -> only authentication is required, no specific permission.
        @GetMapping("/api/v1/test/authenticated-only")
        String open() {
            return "ok";
        }
    }
}
