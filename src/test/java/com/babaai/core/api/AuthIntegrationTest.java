package com.babaai.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.repository.UserRepository;
import com.babaai.core.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerLoginAndFetchCurrentUser() throws Exception {
        String accessToken = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "auth-flow");
        AuthTestSupport.assertMeEndpoint(mockMvc, accessToken, "user_auth-flow");
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "bad-login");

        login("user_bad-login", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void locksAccountAfterFiveFailedAttempts() throws Exception {
        AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "lockout");

        for (int i = 0; i < 5; i++) {
            login("user_lockout", "wrong-password").andExpect(status().isUnauthorized());
        }

        // Now locked — even the correct password is rejected, with a "locked" message.
        login("user_lockout", "password123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", containsString("locked")));
    }

    @Test
    void authenticatedRequestStampsLastSeen() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "presence");
        assertThat(userRepository.findByUsername("user_presence").orElseThrow().getLastSeenAt()).isNull();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThat(userRepository.findByUsername("user_presence").orElseThrow().getLastSeenAt()).isNotNull();
    }

    private ResultActions login(String username, String password) throws Exception {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(form));
    }
}
