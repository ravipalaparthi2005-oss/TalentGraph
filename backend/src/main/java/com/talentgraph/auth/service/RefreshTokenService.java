package com.talentgraph.auth.service;

import com.talentgraph.auth.RefreshToken;
import com.talentgraph.auth.RefreshTokenRepository;
import com.talentgraph.auth.User;
import com.talentgraph.common.exception.UnauthorizedException;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenDays;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @org.springframework.beans.factory.annotation.Value("${jwt.refresh-token-days:7}") long refreshTokenDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Value
    public static class TokenPair {
        String rawRefreshToken;
        RefreshToken refreshTokenEntity;
    }

    @Transactional
    public TokenPair createRefreshToken(User user, String ipAddress, String userAgent) {
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(refreshTokenDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .createdIp(ipAddress)
                .userAgent(userAgent)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        return new TokenPair(rawToken, saved);
    }

    @Transactional
    public TokenPair rotateRefreshToken(String rawToken, String ipAddress, String userAgent) {
        String tokenHash = hashToken(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked()) {
            // Token reuse detected! Revoke all tokens for this user for security.
            refreshTokenRepository.revokeAllUserTokens(token.getUser().getId(), Instant.now());
            throw new UnauthorizedException("Invalid session: Refresh token reuse detected");
        }

        if (token.isExpired()) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        Instant now = Instant.now();
        token.setRevokedAt(now);

        TokenPair nextPair = createRefreshToken(token.getUser(), ipAddress, userAgent);
        token.setReplacedByToken(nextPair.getRefreshTokenEntity());
        refreshTokenRepository.save(token);

        return nextPair;
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
