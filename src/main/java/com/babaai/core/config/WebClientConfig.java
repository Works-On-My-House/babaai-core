package com.babaai.core.config;

import com.babaai.core.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient aiWebClient(AppProperties appProperties) {
        return WebClient.builder()
                .baseUrl(appProperties.getAi().getBaseUrl())
                .defaultHeader("X-Service-Token", appProperties.getAi().getServiceToken())
                .build();
    }
}
