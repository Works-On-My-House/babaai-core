package com.babaai.core.api;

import com.babaai.core.dto.Dtos;
import com.babaai.core.security.SecurityUtils;
import com.babaai.core.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.UserResponse register(@Valid @RequestBody Dtos.UserCreateRequest request) {
        return authService.register(request);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dtos.TokenResponse login(
            @RequestParam("username") String username,
            @RequestParam("password") String password
    ) {
        return authService.login(username, password);
    }

    @GetMapping("/me")
    public Dtos.UserResponse me() {
        return authService.currentUser(SecurityUtils.requireUser());
    }
}
