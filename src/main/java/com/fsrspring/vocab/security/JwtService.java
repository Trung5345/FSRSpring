package com.fsrspring.vocab.security;

import com.fsrspring.vocab.model.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "typ";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final String secret;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;
    private final Clock clock;

    @Autowired
    public JwtService(
            @Value("${app.jwt.secret:}") String secret,
            @Value("${app.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this(secret, accessTokenExpirationMs, refreshTokenExpirationMs, Clock.systemUTC());
    }

    JwtService(String secret, long accessTokenExpirationMs, long refreshTokenExpirationMs, Clock clock) {
        this.secret = secret == null ? "" : secret.trim();
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.clock = clock;
    }

    public boolean isConfigured() {
        return secret.getBytes(StandardCharsets.UTF_8).length >= 32;
    }

    public String generateAccessToken(AppUser user) {
        return generateToken(user, accessTokenExpirationMs, ACCESS_TOKEN_TYPE);
    }

    public String generateRefreshToken(AppUser user) {
        return generateToken(user, refreshTokenExpirationMs, REFRESH_TOKEN_TYPE);
    }

    public boolean isValidAccessToken(String token) {
        return isValidTokenOfType(token, ACCESS_TOKEN_TYPE);
    }

    public boolean isValidRefreshToken(String token) {
        return isValidTokenOfType(token, REFRESH_TOKEN_TYPE);
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    private String generateToken(AppUser user, long expirationMs, String tokenType) {
        Date now = Date.from(clock.instant());
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    private boolean isValidTokenOfType(String token, String expectedType) {
        if (!isConfigured()) {
            return false;
        }
        try {
            Claims claims = extractClaims(token);
            return expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        if (!isConfigured()) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
