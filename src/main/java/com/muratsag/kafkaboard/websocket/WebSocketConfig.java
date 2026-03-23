package com.muratsag.kafkaboard.websocket;

import com.muratsag.kafkaboard.auth.AppUserDetailsService;
import com.muratsag.kafkaboard.auth.AuthenticatedUser;
import com.muratsag.kafkaboard.auth.JwtService;
import com.muratsag.kafkaboard.cluster.ClusterEntity;
import com.muratsag.kafkaboard.cluster.ClusterService;
import com.muratsag.kafkaboard.exception.ForbiddenException;
import com.muratsag.kafkaboard.exception.ResourceNotFoundException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final LagWebSocketHandler lagWebSocketHandler;
    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final ClusterService clusterService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(lagWebSocketHandler, "/ws/clusters/{clusterId}/lag")
                .addInterceptors(new JwtClusterHandshakeInterceptor())
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "https://*.vercel.app",
                        "https://muratsag.online"
                );
    }

    private final class JwtClusterHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes
        ) {
            try {
                if (!(request instanceof ServletServerHttpRequest servletRequest)) {
                    setStatus(response, HttpStatus.BAD_REQUEST);
                    return false;
                }

                String token = extractQueryParam(request.getURI(), "token");
                String clusterIdParam = extractClusterId(servletRequest);

                if (token == null || token.isBlank() || clusterIdParam == null || clusterIdParam.isBlank()) {
                    setStatus(response, HttpStatus.UNAUTHORIZED);
                    return false;
                }

                String username = jwtService.extractUsername(token);
                AuthenticatedUser user = (AuthenticatedUser) userDetailsService.loadUserByUsername(username);
                if (!jwtService.isTokenValid(token, user)) {
                    setStatus(response, HttpStatus.UNAUTHORIZED);
                    return false;
                }

                UUID userId = jwtService.extractUserId(token);
                UUID clusterId = UUID.fromString(clusterIdParam);
                ClusterEntity cluster = clusterService.getOwnedCluster(clusterId, userId);

                attributes.put("userId", userId);
                attributes.put("clusterId", clusterId);
                attributes.put("bootstrapServers", cluster.getBootstrapServers());
                return true;
            } catch (JwtException | IllegalArgumentException e) {
                setStatus(response, HttpStatus.UNAUTHORIZED);
                return false;
            } catch (ForbiddenException e) {
                setStatus(response, HttpStatus.FORBIDDEN);
                return false;
            } catch (ResourceNotFoundException e) {
                setStatus(response, HttpStatus.NOT_FOUND);
                return false;
            }
        }

        @Override
        public void afterHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception
        ) {
            // No-op.
        }

        private String extractClusterId(ServletServerHttpRequest request) {
            String uri = request.getServletRequest().getRequestURI();
            String prefix = "/ws/clusters/";
            String suffix = "/lag";

            if (!uri.startsWith(prefix) || !uri.endsWith(suffix)) {
                return null;
            }

            return uri.substring(prefix.length(), uri.length() - suffix.length());
        }

        private String extractQueryParam(URI uri, String name) {
            String query = uri.getQuery();
            if (query == null || query.isBlank()) {
                return null;
            }

            for (String part : query.split("&")) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2 && keyValue[0].equals(name)) {
                    return URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                }
            }

            return null;
        }

        private void setStatus(ServerHttpResponse response, HttpStatus status) {
            response.setStatusCode(status);
            if (response instanceof ServletServerHttpResponse servletResponse) {
                servletResponse.getServletResponse().setStatus(status.value());
            }
        }
    }
}
