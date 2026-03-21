package com.muratsag.kafkaboard.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByToken(String token);

    void deleteByToken(String token);

    long countByUser_Id(UUID userId);

    void deleteAllByUser_Id(UUID userId);

    void deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
