package com.fsrspring.vocab.service;

import com.fsrspring.vocab.dto.AuthResponse;
import com.fsrspring.vocab.dto.LoginRequest;
import com.fsrspring.vocab.dto.RefreshTokenRequest;
import com.fsrspring.vocab.dto.RegisterRequest;
import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.repository.AppUserRepository;
import com.fsrspring.vocab.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        requireJwtConfigured();
        String email = request.email().trim().toLowerCase();
        if (appUserRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        AppUser user = AppUser.builder()
                .email(email)
                .name(normalizeName(request.name(), email))
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(AppUser.Role.USER)
                .createdAt(now)
                .lastLoginAt(now)
                .build();

        return issueTokens(appUserRepository.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        requireJwtConfigured();
        AppUser user = appUserRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> unauthorized());

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw unauthorized();
        }

        user.setLastLoginAt(LocalDateTime.now());
        return issueTokens(appUserRepository.save(user));
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        requireJwtConfigured();
        if (!jwtService.isValidRefreshToken(request.refreshToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        String email = jwtService.extractEmail(request.refreshToken());
        AppUser user = appUserRepository.findByEmail(email).orElseThrow(this::unauthorized);

        return issueTokens(user);
    }

    private AuthResponse issueTokens(AppUser user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                TOKEN_TYPE
        );
    }

    private void requireJwtConfigured() {
        if (!jwtService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "JWT auth is not configured");
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect");
    }

    private String normalizeName(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return email.split("@")[0];
    }
}
