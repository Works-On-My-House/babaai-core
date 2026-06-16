package com.babaai.core.service;

import com.babaai.core.domain.User;
import com.babaai.core.dto.Dtos;
import com.babaai.core.exception.AppException;
import com.babaai.core.repository.UserRepository;
import com.babaai.core.security.JwtService;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    // Failed-login lockout: lock the account for LOCK_DURATION after MAX_FAILED_ATTEMPTS consecutive
    // wrong passwords.
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public Dtos.UserResponse register(Dtos.UserCreateRequest request) {
        if (userRepository.existsByEmailOrUsername(request.email(), request.username())) {
            throw new AppException("Email or username already registered", HttpStatus.BAD_REQUEST);
        }
        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setHashedPassword(passwordEncoder.encode(request.password()));
        // No default role assigned yet — once roles are defined in AppRole, look it up and
        // do user.getRoles().add(defaultRole) here.
        return DtoMapper.toUserResponse(userRepository.save(user));
    }

    // noRollbackFor: a wrong password / locked account throws AppException, but we must still COMMIT
    // the failed-attempt counter and lock timestamp (default @Transactional rolls back on it).
    @Transactional(noRollbackFor = AppException.class)
    public Dtos.TokenResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Incorrect username or password", HttpStatus.UNAUTHORIZED));

        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new AppException(
                    "Account is temporarily locked due to too many failed attempts. Try again later.",
                    HttpStatus.UNAUTHORIZED);
        }

        if (!passwordEncoder.matches(password, user.getHashedPassword())) {
            registerFailedAttempt(user, now);
            throw new AppException("Incorrect username or password", HttpStatus.UNAUTHORIZED);
        }

        // Success — clear any prior failed-attempt / lock state.
        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
        return new Dtos.TokenResponse(jwtService.createAccessToken(user.getId()));
    }

    public Dtos.UserResponse currentUser(User user) {
        return DtoMapper.toUserResponse(user);
    }

    private void registerFailedAttempt(User user, Instant now) {
        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(now.plus(LOCK_DURATION));
        } else {
            user.setFailedLoginAttempts(attempts);
        }
        userRepository.save(user);
    }
}
