package com.muratsag.kafkaboard.auth;

import com.muratsag.kafkaboard.auth.dto.AuthResponse;
import com.muratsag.kafkaboard.auth.dto.LoginRequest;
import com.muratsag.kafkaboard.auth.dto.RefreshTokenRequest;
import com.muratsag.kafkaboard.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "kafkaboard_refresh_token";

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.register(request);
        writeRefreshTokenCookie(httpRequest, httpResponse, response.getRefreshToken(), false);
        response.setRefreshToken(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthResponse response = authService.login(request);
        writeRefreshTokenCookie(httpRequest, httpResponse, response.getRefreshToken(), false);
        response.setRefreshToken(null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String refreshToken = resolveRefreshToken(request, httpRequest);
        AuthResponse response = authService.refresh(new RefreshTokenRequest(refreshToken));
        writeRefreshTokenCookie(httpRequest, httpResponse, response.getRefreshToken(), false);
        response.setRefreshToken(null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        authService.logout(new RefreshTokenRequest(resolveRefreshToken(request, httpRequest)));
        writeRefreshTokenCookie(httpRequest, httpResponse, "", true);
        return ResponseEntity.ok().build();
    }

    private String resolveRefreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            return request.getRefreshToken();
        }

        if (httpRequest.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : httpRequest.getCookies()) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void writeRefreshTokenCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            String refreshToken,
            boolean clear
    ) {
        boolean secure = isSecureRequest(request);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, clear ? "" : refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/api/auth")
                .maxAge(clear ? Duration.ZERO : Duration.ofDays(7))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }
}
