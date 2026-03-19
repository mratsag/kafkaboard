package com.muratsag.kafkaboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PartitionLagDto {
    private String topic;
    private int partition;
    private long currentOffset;   // consumer nerede
    private long endOffset;       // topic'in sonu
    private long lag;             // fark
}