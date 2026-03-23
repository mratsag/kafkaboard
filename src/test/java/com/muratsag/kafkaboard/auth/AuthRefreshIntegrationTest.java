package com.muratsag.kafkaboard.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muratsag.kafkaboard.auth.dto.LoginRequest;
import com.muratsag.kafkaboard.auth.dto.RefreshTokenRequest;
import com.muratsag.kafkaboard.auth.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRefreshIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void shouldReturnAccessAndRefreshTokenOnRegisterAndLogin() throws Exception {
        String email = "auth-" + UUID.randomUUID() + "@test.com";
        register(email, "Test1234!");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("Test1234!");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getCookie("kafkaboard_refresh_token")).isNotNull();
    }

    @Test
    void shouldRefreshAccessTokenAndRevokeOnLogout() throws Exception {
        AuthResult authResponse = register("refresh-" + UUID.randomUUID() + "@test.com", "Test1234!");
        String refreshToken = authResponse.refreshToken();
        Cookie refreshCookie = authResponse.refreshCookie();

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest())))
                .andExpect(status().isOk());

        assertThat(refreshTokenRepository.findByToken(refreshToken)).isEmpty();

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldStoreMultipleRefreshTokensForSameUser() throws Exception {
        String email = "multi-" + UUID.randomUUID() + "@test.com";
        register(email, "Test1234!");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword("Test1234!");

        AuthResult firstLogin = login(loginRequest);
        AuthResult secondLogin = login(loginRequest);

        assertThat(firstLogin.refreshToken()).isNotEqualTo(secondLogin.refreshToken());
        assertThat(refreshTokenRepository.count()).isGreaterThanOrEqualTo(3);
    }

    private AuthResult register(String email, String password) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        return toAuthResult(result);
    }

    private AuthResult login(LoginRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        return toAuthResult(result);
    }

    private AuthResult toAuthResult(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Cookie refreshCookie = result.getResponse().getCookie("kafkaboard_refresh_token");
        assertThat(refreshCookie).isNotNull();

        return new AuthResult(
                body.get("token").asText(),
                refreshCookie.getValue(),
                refreshCookie
        );
    }

    private record AuthResult(String token, String refreshToken, Cookie refreshCookie) {
    }
}
