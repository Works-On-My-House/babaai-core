package com.babaai.core.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.domain.User;
import com.babaai.core.repository.UserRepository;
import com.babaai.core.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Verifies the {@code enabled} (and admin-lock) account-status switch is actually enforced — both at
 * login and per-request — so disabling a user stops them acting on core endpoints immediately.
 */
class AccountStatusEnforcementTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private void setEnabled(String username, boolean enabled) {
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Test
    void disabledUserCannotLogIn() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "disabled-login");
        // Sanity: enabled user's token works.
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        setEnabled("user_disabled-login", false);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", "user_disabled-login");
        form.add("password", "password123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(form))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledUserTokenIsRejectedPerRequest() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "disabled-req");
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        setEnabled("user_disabled-req", false);

        // Same (still cryptographically valid) token must now be rejected with an explicit reason —
        // the filter reloads the user every request and 403s a disabled account.
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.detail").value("Account is disabled"));
    }
}
