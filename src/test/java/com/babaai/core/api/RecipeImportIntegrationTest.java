package com.babaai.core.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.babaai.core.AbstractIntegrationTest;
import com.babaai.core.support.AuthTestSupport;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** User recipe-import submission (ClickUp 869dtx7uq): upload stages a pending import the user can see. */
class RecipeImportIntegrationTest extends AbstractIntegrationTest {

    private MockMultipartFile jsonFile(byte[] body) {
        return new MockMultipartFile("file", "recipes.json", "application/json", body);
    }

    @Test
    void userSubmitsFileAndSeesItInMine() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "importer");
        byte[] body = "[{\"name\":\"Test Import\"}]".getBytes(StandardCharsets.UTF_8);

        mockMvc.perform(multipart("/api/v1/recipe-imports")
                        .file(jsonFile(body))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.original_filename").value("recipes.json"))
                .andExpect(jsonPath("$.content_type").value("application/json"))
                .andExpect(jsonPath("$.size_bytes").value(body.length));

        mockMvc.perform(get("/api/v1/recipe-imports/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].original_filename").value("recipes.json"))
                .andExpect(jsonPath("$.items[0].status").value("pending"));
    }

    @Test
    void submissionRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/v1/recipe-imports").file(jsonFile("x".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyFileIsRejected() throws Exception {
        String token = AuthTestSupport.registerAndLogin(mockMvc, objectMapper, "importer-empty");
        mockMvc.perform(multipart("/api/v1/recipe-imports")
                        .file(jsonFile(new byte[0]))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
