package com.babaai.core.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class AuthTestSupport {

    private AuthTestSupport() {
    }

    public static String registerAndLogin(MockMvc mockMvc, ObjectMapper objectMapper, String suffix) throws Exception {
        String email = "user-" + suffix + "@example.com";
        String username = "user_" + suffix;
        String password = "password123";

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "%s",
                                          "username": "%s",
                                          "password": "%s"
                                        }
                                        """.formatted(email, username, password)))
                .andExpect(status().isCreated());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);

        MvcResult loginResult = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .params(form))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode tokenBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return tokenBody.get("access_token").asText();
    }

    public static void assertMeEndpoint(MockMvc mockMvc, String accessToken, String expectedUsername) throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.username")
                        .value(expectedUsername));
    }
}
