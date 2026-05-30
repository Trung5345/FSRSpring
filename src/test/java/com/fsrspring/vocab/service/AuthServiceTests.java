package com.fsrspring.vocab.service;

import com.fsrspring.vocab.dto.LoginRequest;
import com.fsrspring.vocab.dto.RegisterRequest;
import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.repository.AppUserRepository;
import com.fsrspring.vocab.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesLocalUserWithHashedPasswordAndIssuesTokens() {
        when(jwtService.isConfigured()).thenReturn(true);
        when(passwordEncoder.encode("strong-password")).thenReturn("hashed");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(jwtService.generateAccessToken(any(AppUser.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(AppUser.class))).thenReturn("refresh");

        var response = authService.register(new RegisterRequest("ALICE@example.com", "strong-password", " Alice "));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(AppUser.Role.USER);
    }

    @Test
    void loginRejectsOAuthOnlyUserWithoutPasswordHash() {
        when(jwtService.isConfigured()).thenReturn(true);
        when(appUserRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(AppUser.builder().email("alice@example.com").build()));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@example.com", "password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }

    @Test
    void loginIssuesTokensForPasswordUser() {
        AppUser user = AppUser.builder()
                .id(1L)
                .email("alice@example.com")
                .passwordHash("hashed")
                .role(AppUser.Role.ADMIN)
                .build();
        when(jwtService.isConfigured()).thenReturn(true);
        when(appUserRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(appUserRepository.save(user)).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");

        var response = authService.login(new LoginRequest("alice@example.com", "password"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }
}
