package com.babaai.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // These WebClients use a default (camelCase) Jackson mapper, so cross-wire DTOs must pin their
    // field names with @JsonProperty (the convention used throughout RecipeDtos) — see ProposalRequest.

    @Bean
    public WebClient aiWebClient(AppProperties appProperties) {
        return WebClient.builder()
                .baseUrl(appProperties.getAi().getBaseUrl())
                .defaultHeader("X-Service-Token", appProperties.getAi().getServiceToken())
                .build();
    }

    @Bean
    public WebClient gatewayWebClient(AppProperties appProperties) {
        // No static auth header: each call carries a freshly-signed service token (see
        // GatewayRevocationClient), so nothing reusable is sent on the wire.
        return WebClient.builder()
                .baseUrl(appProperties.getGateway().getBaseUrl())
                .build();
    }
}
