package com.muratsag.kafkaboard.controller;

import com.muratsag.kafkaboard.dto.ConsumerGroupInfoDto;
import com.muratsag.kafkaboard.dto.CreateTopicRequest;
import com.muratsag.kafkaboard.dto.TopicMessageDto;
import com.muratsag.kafkaboard.dto.TopicInfoDto;
import com.muratsag.kafkaboard.service.KafkaConsumerGroupService;
import com.muratsag.kafkaboard.service.KafkaTopicMessageService;
import com.muratsag.kafkaboard.service.KafkaTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cluster")
@RequiredArgsConstructor
public class ClusterController {

    private final KafkaTopicService kafkaTopicService;
    private final KafkaConsumerGroupService kafkaConsumerGroupService;
    private final KafkaTopicMessageService kafkaTopicMessageService;

    @GetMapping("/topics")
    public List<TopicInfoDto> getTopics(@RequestParam String bootstrapServers) {
        return kafkaTopicService.getTopics(bootstrapServers);
    }

    @PostMapping("/topics")
    public ResponseEntity<TopicInfoDto> createTopic(@RequestBody CreateTopicRequest request) {
        TopicInfoDto created = kafkaTopicService.createTopic(
            request.getBootstrapServers(),
            request.getTopicName(),
            request.getPartitions(),
            request.getReplicationFactor()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/topics/{topicName}")
    public ResponseEntity<Void> deleteTopic(
            @RequestParam String bootstrapServers,
            @PathVariable String topicName) {
        kafkaTopicService.deleteTopic(bootstrapServers, topicName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/topics/{topicName}/messages")
    public List<TopicMessageDto> getTopicMessages(
            @RequestParam String bootstrapServers,
            @RequestParam(defaultValue = "10") int limit,
            @PathVariable String topicName) {
        return kafkaTopicMessageService.getLatestMessages(bootstrapServers, topicName, limit);
    }

    @GetMapping("/consumer-groups")
    public List<ConsumerGroupInfoDto> getConsumerGroups(@RequestParam String bootstrapServers) {
        return kafkaConsumerGroupService.getConsumerGroupLag(bootstrapServers);
    }
}
