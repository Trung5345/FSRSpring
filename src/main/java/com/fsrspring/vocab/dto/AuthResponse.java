package com.fsrspring.vocab.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
