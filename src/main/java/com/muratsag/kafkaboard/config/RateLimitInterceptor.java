package com.muratsag.kafkaboard.config;

import com.muratsag.kafkaboard.exception.TooManyRequestsException;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String ipAddress = extractClientIp(request);

        ConsumptionProbe probe;
        long retryAfterSeconds;

        if ("/api/auth/login".equals(path)) {
            probe = rateLimitService.consumeLoginRequest(ipAddress);
            retryAfterSeconds = 300;
        } else if ("/api/auth/register".equals(path)) {
            probe = rateLimitService.consumeRegisterRequest(ipAddress);
            retryAfterSeconds = 3600;
        } else {
            probe = rateLimitService.consumeApiRequest(ipAddress);
            retryAfterSeconds = 60;
        }

        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        if (!probe.isConsumed()) {
            throw new TooManyRequestsException("Çok fazla istek. 5 dakika sonra tekrar deneyin.", retryAfterSeconds);
        }

        return true;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
