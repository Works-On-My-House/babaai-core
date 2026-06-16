package com.babaai.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class RefreshTokenIntegrationTest extends AbstractIntegrationTest {

    private static final String COOKIE = "refresh_token";

    @Test
    void refreshRotatesTheTokenAndIssuesNewAccess() throws Exception {
        Cookie refresh = login("rotate").cookie();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andReturn();

        Cookie rotated = result.getResponse().getCookie(COOKIE);
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(refresh.getValue());

        // The rotated token works.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotated)).andExpect(status().isOk());
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeFamily() throws Exception {
        Cookie refresh = login("reuse").cookie();

        Cookie rotated = mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(COOKIE);

        // Present the OLD (already-rotated) token again -> reuse detected.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized());

        // Family revoked -> the rotated token is dead too.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotated))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesTheRefreshToken() throws Exception {
        Cookie refresh = login("logout").cookie();

        mockMvc.perform(post("/api/v1/auth/logout").cookie(refresh)).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenCarriesPermissionsAndVersionClaims() throws Exception {
        String payload = new String(
                Base64.getUrlDecoder().decode(login("claims").accessToken().split("\\.")[1]),
                StandardCharsets.UTF_8);
        assertThat(payload).contains("\"permissions\"").contains("\"pv\"");
    }

    private record LoginOutcome(Cookie cookie, String accessToken) {}

    private LoginOutcome login(String suffix) throws Exception {
        String username = "user_" + suffix;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s@example.com","username":"%s","password":"password123"}
                                """.formatted(username, username)))
                .andExpect(status().isCreated());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", "password123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .params(form))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(COOKIE);
        assertThat(cookie).isNotNull();
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("access_token").asString();
        return new LoginOutcome(cookie, accessToken);
    }
}
