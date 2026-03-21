package com.muratsag.kafkaboard.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muratsag.kafkaboard.auth.dto.LoginRequest;
import com.muratsag.kafkaboard.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitInterceptorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRateLimitLoginRequestsPerIp() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@test.com");
        request.setPassword("wrong-password");

        for (int attempt = 0; attempt < 10; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "10.0.0.1")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "10.0.0.1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(jsonPath("$.error").value("Çok fazla istek. Lütfen bekleyin."))
                .andExpect(jsonPath("$.retryAfter").value(300));
    }

    @Test
    void shouldExposeRemainingRateLimitHeaderOnRegisterRequests() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("register-" + UUID.randomUUID() + "@test.com");
        request.setPassword("Test1234!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "10.0.0.2")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-RateLimit-Remaining", "4"));
    }

    @Test
    void shouldApplyApiRateLimitToAuthenticatedRequests() throws Exception {
        String token = registerAndExtractToken("api-user-" + UUID.randomUUID() + "@test.com", "10.0.0.3");

        mockMvc.perform(get("/api/clusters")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-For", "10.0.0.4"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "59"));
    }

    private String registerAndExtractToken(String email, String ipAddress) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("Test1234!");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", ipAddress)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
