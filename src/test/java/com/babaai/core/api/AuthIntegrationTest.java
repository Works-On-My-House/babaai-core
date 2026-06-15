package com.babaai.core.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.support.AuthTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerLoginAndFetchCurrentUser() throws Exception {
        String accessToken = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "auth-flow");
        AuthTestSupport.assertMeEndpoint(mockMvc, accessToken, "user_auth-flow");
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "bad-login");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", "user_bad-login");
        form.add("password", "wrong-password");

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .params(form))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").exists());
    }
}
