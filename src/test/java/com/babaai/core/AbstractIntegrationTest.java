package com.babaai.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    private static Path jwtKeyDir;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected ObjectMapper objectMapper;

    protected MockMvc mockMvc;

    @BeforeAll
    static void startPostgres() {
        PostgresTestContainer.get();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
        var postgres = PostgresTestContainer.get();

        if (jwtKeyDir == null) {
            jwtKeyDir = Files.createTempDirectory("babaai-jwt-keys");
        }

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("JWT_EXPIRE_MINUTES", () -> "60");
        registry.add("JWT_KEY_PATH", () -> jwtKeyDir.toString());
        registry.add("CORS_ORIGINS", () -> "http://localhost:5173");
        registry.add("AI_SERVICE_URL", () -> "http://localhost:9999");
        registry.add("AI_SERVICE_TOKEN", () -> "test-ai-token");
        registry.add("GATEWAY_INTERNAL_URL", () -> "http://localhost:9998");
        registry.add("DEFAULT_MIN_MATCH_PERCENT", () -> "50");
        registry.add("DEFAULT_SUGGESTION_LIMIT", () -> "20");
        registry.add("DEFAULT_PAGE_SIZE", () -> "20");
        registry.add("DEFAULT_INGREDIENT_CATEGORY", () -> "Other");
    }

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }
}
