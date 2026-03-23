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
    private final ClientIpResolver clientIpResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String ipAddress = clientIpResolver.resolve(request);

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
}
