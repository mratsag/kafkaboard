package com.muratsag.kafkaboard.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ConsumerGroupInfoDto {
    private String groupId;
    private String state;         // STABLE, EMPTY, DEAD gibi
    private long totalLag;        // toplam lag
    private List<PartitionLagDto> partitionLags;
}