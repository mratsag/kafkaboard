package com.muratsag.kafkaboard.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muratsag.kafkaboard.auth.RefreshTokenRepository;
import com.muratsag.kafkaboard.auth.dto.RegisterRequest;
import com.muratsag.kafkaboard.cluster.ClusterRepository;
import com.muratsag.kafkaboard.cluster.dto.CreateClusterRequest;
import com.muratsag.kafkaboard.profile.dto.DeleteAccountRequest;
import com.muratsag.kafkaboard.profile.dto.UpdateDisplayNameRequest;
import com.muratsag.kafkaboard.profile.dto.UpdatePasswordRequest;
import com.muratsag.kafkaboard.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void shouldReturnProfile() throws Exception {
        JsonNode auth = register("profile-" + UUID.randomUUID() + "@test.com", "Test1234!");

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + auth.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(auth.get("email").asText()))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldUpdateDisplayName() throws Exception {
        JsonNode auth = register("display-" + UUID.randomUUID() + "@test.com", "Test1234!");

        UpdateDisplayNameRequest request = new UpdateDisplayNameRequest();
        request.setDisplayName("Murat Sag");

        mockMvc.perform(put("/api/profile/display-name")
                        .header("Authorization", "Bearer " + auth.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Murat Sag"));
    }

    @Test
    void shouldRejectWrongCurrentPasswordOnPasswordUpdate() throws Exception {
        JsonNode auth = register("password-" + UUID.randomUUID() + "@test.com", "Test1234!");

        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("NewPassword123!");

        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", "Bearer " + auth.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteAccountAndCascadeClustersAndRefreshTokens() throws Exception {
        JsonNode auth = register("delete-" + UUID.randomUUID() + "@test.com", "Test1234!");
        String token = auth.get("token").asText();
        UUID userId = UUID.fromString(auth.get("userId").asText());

        CreateClusterRequest createClusterRequest = new CreateClusterRequest();
        createClusterRequest.setName("Local Kafka");
        createClusterRequest.setBootstrapServers("localhost:9092");

        mockMvc.perform(post("/api/clusters")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createClusterRequest)))
                .andExpect(status().isCreated());

        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword("Test1234!");

        mockMvc.perform(delete("/api/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByEmail(auth.get("email").asText())).isEmpty();
        assertThat(clusterRepository.countByUser_Id(userId)).isZero();
        assertThat(refreshTokenRepository.countByUser_Id(userId)).isZero();
    }

    private JsonNode register(String email, String password) throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        ((com.fasterxml.jackson.databind.node.ObjectNode) response).put("email", email.trim().toLowerCase());
        String userId = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow()
                .getId()
                .toString();
        ((com.fasterxml.jackson.databind.node.ObjectNode) response).put("userId", userId);
        return response;
    }
}
