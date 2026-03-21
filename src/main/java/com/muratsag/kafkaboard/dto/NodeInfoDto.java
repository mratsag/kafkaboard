package com.muratsag.kafkaboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeInfoDto {
    private int nodeId;
    private String host;
    private int port;
    private String rack;
}
