package com.babaai.core.config;

import com.babaai.core.security.JwtAuthenticationFilter;
import com.babaai.core.security.ServiceTokenFilter;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize("hasAuthority('...')") for permission-based authorization
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ServiceTokenFilter serviceTokenFilter;
    private final AppProperties appProperties;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ServiceTokenFilter serviceTokenFilter,
            AppProperties appProperties
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.serviceTokenFilter = serviceTokenFilter;
        this.appProperties = appProperties;
    }

    /** A public (no-auth) endpoint. {@code method == null} means any HTTP method. */
    private record PublicEndpoint(HttpMethod method, String pattern) {}

    private static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            new PublicEndpoint(null, "/.well-known/jwks.json"),
            new PublicEndpoint(null, "/api/v1/health"),
            new PublicEndpoint(null, "/api/v1/config/**"),
            new PublicEndpoint(HttpMethod.POST, "/api/v1/auth/register"),
            new PublicEndpoint(HttpMethod.POST, "/api/v1/auth/login"),
            new PublicEndpoint(HttpMethod.POST, "/api/v1/auth/refresh"),
            new PublicEndpoint(HttpMethod.POST, "/api/v1/auth/logout"),
            new PublicEndpoint(HttpMethod.GET, "/api/v1/recipes"),
            new PublicEndpoint(HttpMethod.GET, "/api/v1/recipes/categories"),
            new PublicEndpoint(HttpMethod.GET, "/api/v1/recipes/featured"),
            new PublicEndpoint(HttpMethod.GET, "/api/v1/recipes/*"),
            new PublicEndpoint(HttpMethod.POST, "/api/v1/recipes/*/view"));

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    PUBLIC_ENDPOINTS.forEach(endpoint -> {
                        if (endpoint.method() == null) {
                            auth.requestMatchers(endpoint.pattern()).permitAll();
                        } else {
                            auth.requestMatchers(endpoint.method(), endpoint.pattern()).permitAll();
                        }
                    });
                    auth.requestMatchers("/api/v1/internal/**").hasAuthority(ServiceTokenFilter.SERVICE_AUTHORITY)
                            .anyRequest().authenticated();
                })
                // Unauthenticated (no/expired/invalid token) -> 401; authenticated but lacking the
                // required permission -> 403. Both return a JSON {"detail": ...} the frontend can show.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"detail\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"detail\":\"You do not have permission to perform this action\"}");
                        }))
                .addFilterBefore(serviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Resolves the {value} template in @HasPermission -> @PreAuthorize("hasAuthority('{value}')").
    @Bean
    public AnnotationTemplateExpressionDefaults annotationTemplateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(appProperties.getCors().getAllowedOrigins().split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
