package com.babaai.core.api;

import com.babaai.core.config.AppProperties;
import com.babaai.core.dto.Dtos;
import com.babaai.core.exception.AppException;
import com.babaai.core.security.SecurityUtils;
import com.babaai.core.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AppProperties appProperties;

    public AuthController(AuthService authService, AppProperties appProperties) {
        this.authService = authService;
        this.appProperties = appProperties;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.UserResponse register(@Valid @RequestBody Dtos.UserCreateRequest request) {
        return authService.register(request);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dtos.TokenResponse login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletResponse response
    ) {
        AuthService.IssuedTokens tokens = authService.login(username, password);
        setRefreshCookie(response, tokens.refreshToken());
        return new Dtos.TokenResponse(tokens.accessToken());
    }

    @PostMapping("/refresh")
    public Dtos.TokenResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readRefreshCookie(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AppException("Missing refresh token", HttpStatus.UNAUTHORIZED);
        }
        AuthService.IssuedTokens tokens = authService.refresh(refreshToken);
        setRefreshCookie(response, tokens.refreshToken());
        return new Dtos.TokenResponse(tokens.accessToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readRefreshCookie(request);
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        clearRefreshCookie(response);
    }

    @GetMapping("/me")
    public Dtos.UserResponse me() {
        return authService.currentUser(SecurityUtils.requireUser());
    }

    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        String name = appProperties.getJwt().getRefreshCookieName();
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setRefreshCookie(HttpServletResponse response, String value) {
        AppProperties.Jwt jwt = appProperties.getJwt();
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(jwt, value,
                Duration.ofDays(jwt.getRefreshExpireDays())).toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        AppProperties.Jwt jwt = appProperties.getJwt();
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(jwt, "", Duration.ZERO).toString());
    }

    private ResponseCookie buildCookie(AppProperties.Jwt jwt, String value, Duration maxAge) {
        return ResponseCookie.from(jwt.getRefreshCookieName(), value)
                .httpOnly(true)
                .secure(jwt.isRefreshCookieSecure())
                .sameSite(jwt.getRefreshCookieSameSite())
                .path(jwt.getRefreshCookiePath())
                .maxAge(maxAge)
                .build();
    }
}
