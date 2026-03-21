package com.muratsag.kafkaboard.profile.dto;

import lombok.Data;

@Data
public class UpdateEmailRequest {
    private String newEmail;
    private String password;
}
