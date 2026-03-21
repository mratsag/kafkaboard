package com.muratsag.kafkaboard.auth;

import com.muratsag.kafkaboard.user.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public String generateRefreshToken(UserEntity user) {
        RefreshTokenEntity refreshToken = refreshTokenRepository.save(
                RefreshTokenEntity.builder()
                        .user(user)
                        .token(UUID.randomUUID().toString())
                        .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpiration)))
                        .build()
        );

        return refreshToken.getToken();
    }

    @Transactional
    public RefreshTokenEntity validateRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Refresh token geçersiz veya süresi dolmuş");
        }

        RefreshTokenEntity refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Refresh token geçersiz veya süresi dolmuş"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadCredentialsException("Refresh token geçersiz veya süresi dolmuş");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.deleteAllByUser_Id(userId);
    }

    @Scheduled(fixedDelay = 86400000)
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
