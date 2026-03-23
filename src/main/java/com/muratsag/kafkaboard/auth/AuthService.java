package com.muratsag.kafkaboard.auth;

import com.muratsag.kafkaboard.auth.dto.AuthResponse;
import com.muratsag.kafkaboard.auth.dto.LoginRequest;
import com.muratsag.kafkaboard.auth.dto.RefreshTokenRequest;
import com.muratsag.kafkaboard.auth.dto.RegisterRequest;
import com.muratsag.kafkaboard.exception.ConflictException;
import com.muratsag.kafkaboard.user.UserEntity;
import com.muratsag.kafkaboard.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        validateCredentials(request.getEmail(), request.getPassword());
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Bu email ile kayıtlı kullanıcı zaten var");
        }

        UserEntity user = userRepository.save(
                UserEntity.builder()
                        .email(normalizedEmail)
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .avatarColor(generateAvatarColor(normalizedEmail))
                        .build()
        );

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        validateCredentials(request.getEmail(), request.getPassword());
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Email veya şifre hatalı"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Email veya şifre hatalı");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshTokenEntity refreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());

        return AuthResponse.builder()
                .token(jwtService.generateToken(toAuthenticatedUser(refreshToken.getUser())))
                .refreshToken(refreshToken.getToken())
                .build();
    }

    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
    }

    private void validateCredentials(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email boş olamaz");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Şifre boş olamaz");
        }
    }

    private AuthenticatedUser toAuthenticatedUser(UserEntity user) {
        return AuthenticatedUser.builder()
                .id(user.getId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .build();
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(toAuthenticatedUser(user)))
                .refreshToken(refreshTokenService.generateRefreshToken(user))
                .build();
    }

    private String generateAvatarColor(String seed) {
        String[] palette = {
                "#6366f1",
                "#8b5cf6",
                "#ec4899",
                "#f97316",
                "#14b8a6",
                "#0ea5e9",
                "#22c55e",
                "#eab308"
        };

        int index = Math.abs(seed.hashCode()) % palette.length;
        return palette[index];
    }
}
