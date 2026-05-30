package com.fsrspring.vocab.security;

import com.fsrspring.vocab.model.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTests {

    private final Clock clock = Clock.fixed(java.time.Instant.now(), ZoneOffset.UTC);

    @Test
    void generatesAccessAndRefreshTokensWithSeparateTokenTypes() {
        JwtService jwtService = new JwtService(
                "test-jwt-secret-with-at-least-32-bytes",
                900000,
                604800000,
                clock
        );
        AppUser user = AppUser.builder()
                .id(42L)
                .email("alice@example.com")
                .role(AppUser.Role.USER)
                .build();

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(jwtService.isValidAccessToken(accessToken)).isTrue();
        assertThat(jwtService.isValidRefreshToken(refreshToken)).isTrue();
        assertThat(jwtService.isValidRefreshToken(accessToken)).isFalse();
        assertThat(jwtService.isValidAccessToken(refreshToken)).isFalse();
        assertThat(jwtService.extractEmail(accessToken)).isEqualTo("alice@example.com");
    }

    @Test
    void rejectsTokensWhenSecretIsNotConfigured() {
        JwtService jwtService = new JwtService("", 900000, 604800000, clock);

        assertThat(jwtService.isConfigured()).isFalse();
        assertThat(jwtService.isValidAccessToken("not-a-token")).isFalse();
    }
}
