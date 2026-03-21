package com.muratsag.kafkaboard.profile.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProfileResponse {
    private UUID id;
    private String email;
    private String displayName;
    private String avatarColor;
    private LocalDateTime createdAt;
}
