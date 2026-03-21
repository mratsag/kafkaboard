package com.muratsag.kafkaboard.profile;

import com.muratsag.kafkaboard.auth.AuthenticatedUser;
import com.muratsag.kafkaboard.auth.RefreshTokenService;
import com.muratsag.kafkaboard.auth.dto.AuthResponse;
import com.muratsag.kafkaboard.cluster.ClusterRepository;
import com.muratsag.kafkaboard.profile.dto.ProfileResponse;
import com.muratsag.kafkaboard.profile.dto.UpdateDisplayNameRequest;
import com.muratsag.kafkaboard.profile.dto.UpdateEmailRequest;
import com.muratsag.kafkaboard.profile.dto.UpdatePasswordRequest;
import com.muratsag.kafkaboard.user.UserEntity;
import com.muratsag.kafkaboard.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final ClusterRepository clusterRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.muratsag.kafkaboard.auth.JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Authentication authentication) {
        UserEntity user = getCurrentUser(authentication);
        return toProfileResponse(user);
    }

    @Transactional
    public AuthResponse updateEmail(Authentication authentication, UpdateEmailRequest request) {
        UserEntity user = getCurrentUser(authentication);
        validatePassword(user, request.getPassword());

        String normalizedEmail = normalizeEmail(request.getNewEmail());
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, user.getId())) {
            throw new IllegalArgumentException("Bu email zaten kullanımda");
        }

        user.setEmail(normalizedEmail);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());

        return AuthResponse.builder()
                .token(jwtService.generateToken(toAuthenticatedUser(user)))
                .refreshToken(refreshTokenService.generateRefreshToken(user))
                .build();
    }

    @Transactional
    public void updatePassword(Authentication authentication, UpdatePasswordRequest request) {
        UserEntity user = getCurrentUser(authentication);
        validatePassword(user, request.getCurrentPassword());

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("Yeni şifre boş olamaz");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public ProfileResponse updateDisplayName(Authentication authentication, UpdateDisplayNameRequest request) {
        UserEntity user = getCurrentUser(authentication);
        String displayName = request.getDisplayName() == null ? null : request.getDisplayName().trim();
        if (displayName != null && displayName.length() > 100) {
            throw new IllegalArgumentException("Display name en fazla 100 karakter olabilir");
        }

        user.setDisplayName(displayName == null || displayName.isBlank() ? null : displayName);
        userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public void deleteAccount(Authentication authentication, String password) {
        UserEntity user = getCurrentUser(authentication);
        validatePassword(user, password);
        clusterRepository.deleteAllByUser_Id(user.getId());
        refreshTokenService.revokeAllForUser(user.getId());
        userRepository.delete(user);
    }

    private UserEntity getCurrentUser(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));
    }

    private void validatePassword(UserEntity user, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Şifre boş olamaz");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Şifre doğrulanamadı");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email boş olamaz");
        }
        return email.trim().toLowerCase();
    }

    private ProfileResponse toProfileResponse(UserEntity user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarColor(user.getAvatarColor())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthenticatedUser toAuthenticatedUser(UserEntity user) {
        return AuthenticatedUser.builder()
                .id(user.getId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .build();
    }
}
