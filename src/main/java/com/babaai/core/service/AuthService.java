package com.babaai.core.service;

import com.babaai.core.domain.User;
import com.babaai.core.dto.Dtos;
import com.babaai.core.exception.AppException;
import com.babaai.core.repository.UserRepository;
import com.babaai.core.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

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

    public Dtos.TokenResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("Incorrect username or password", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(password, user.getHashedPassword())) {
            throw new AppException("Incorrect username or password", HttpStatus.UNAUTHORIZED);
        }
        return new Dtos.TokenResponse(jwtService.createAccessToken(user.getId()));
    }

    public Dtos.UserResponse currentUser(User user) {
        return DtoMapper.toUserResponse(user);
    }
}
