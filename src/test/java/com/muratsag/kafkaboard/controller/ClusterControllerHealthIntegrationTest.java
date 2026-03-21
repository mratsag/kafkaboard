package com.muratsag.kafkaboard.controller;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
class ClusterControllerHealthIntegrationTest {

    private static final String TOPIC_NAME = "test-topic";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @BeforeEach
    void setUp() throws Exception {
        resetTopic(TOPIC_NAME);
        produceMessages();
    }

    @Test
    void shouldReturnHealthyStatusForValidCluster() throws Exception {
        mockMvc.perform(get("/api/cluster/health")
                        .param("bootstrapServers", embeddedKafkaBroker.getBrokersAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HEALTHY"))
                .andExpect(jsonPath("$.clusterId", notNullValue()))
                .andExpect(jsonPath("$.nodeCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.nodes", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.topicCount", greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldReturnUnhealthyStatusForInvalidBootstrapServers() throws Exception {
        mockMvc.perform(get("/api/cluster/health")
                        .param("bootstrapServers", "yanlis:9092"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNHEALTHY"))
                .andExpect(jsonPath("$.clusterId").value(nullValue()))
                .andExpect(jsonPath("$.nodeCount").value(0))
                .andExpect(jsonPath("$.nodes", hasSize(0)))
                .andExpect(jsonPath("$.topicCount").value(0));
    }

    @Test
    void shouldReturnBadRequestForBlankBootstrapServers() throws Exception {
        mockMvc.perform(get("/api/cluster/health")
                        .param("bootstrapServers", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bootstrap server adresi boş olamaz"));
    }

    @Test
    void shouldKeepExistingEndpointsWorking() throws Exception {
        mockMvc.perform(get("/api/cluster/topics")
                        .param("bootstrapServers", embeddedKafkaBroker.getBrokersAsString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/cluster/consumer-groups")
                        .param("bootstrapServers", embeddedKafkaBroker.getBrokersAsString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cluster/topics/{topicName}/messages", TOPIC_NAME)
                        .param("bootstrapServers", embeddedKafkaBroker.getBrokersAsString())
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    private void resetTopic(String topicName) throws Exception {
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString()
        );

        try (AdminClient adminClient = AdminClient.create(config)) {
            try {
                adminClient.deleteTopics(List.of(topicName)).all().get(5, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                if (!e.getCause().getClass().getSimpleName().equals("UnknownTopicOrPartitionException")) {
                    throw e;
                }
            }

            try {
                adminClient.createTopics(List.of(new NewTopic(topicName, 1, (short) 1)))
                        .all()
                        .get(5, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                if (!e.getCause().getClass().getSimpleName().equals("TopicExistsException")) {
                    throw e;
                }
            }
        }
    }

    private void produceMessages() throws Exception {
        Map<String, Object> producerConfig = new HashMap<>();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerConfig)) {
            producer.send(new ProducerRecord<>(TOPIC_NAME, "order-1", "{\"orderId\": 1}")).get(5, TimeUnit.SECONDS);
            producer.send(new ProducerRecord<>(TOPIC_NAME, "order-2", "{\"orderId\": 2}")).get(5, TimeUnit.SECONDS);
            producer.send(new ProducerRecord<>(TOPIC_NAME, "order-3", "{\"orderId\": 3}")).get(5, TimeUnit.SECONDS);
            producer.flush();
        }
    }
}
