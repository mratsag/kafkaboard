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
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
class ClusterControllerTopicMessagesIntegrationTest {

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
    void shouldReturnLatestMessagesForExistingTopic() throws Exception {
        mockMvc.perform(get("/api/cluster/topics/{topicName}/messages", TOPIC_NAME)
                        .param("bootstrapServers", embeddedKafkaBroker.getBrokersAsString())
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].key").value("order-6"))
                .andExpect(jsonPath("$[0].value").value("{\"orderId\": 6}"))
                .andExpect(jsonPath("$[0].partition").value(0))
                .andExpect(jsonPath("$[0].offset").value(5))
                .andExpect(jsonPath("$[4].key").value("order-2"))
                .andExpect(jsonPath("$[4].offset").value(1));
    }

    @Test
    void shouldReturnBadRequestForMissingTopic() throws Exception {
        mockMvc.perform(get("/api/cluster/topics/{topicName}/messages", "olmayan-topic")
                        .param("bootstrapServers", embeddedKafkaBroker.getBrokersAsString())
                        .param("limit", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Topic bulunamadı: olmayan-topic"));
    }

    @Test
    void shouldReturnBadRequestForInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/cluster/topics/{topicName}/messages", TOPIC_NAME)
                        .param("bootstrapServers", embeddedKafkaBroker.getBrokersAsString())
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Limit en az 1 olmalı"));
    }

    @Test
    void shouldReturnBadGatewayForInvalidBootstrapServers() throws Exception {
        mockMvc.perform(get("/api/cluster/topics/{topicName}/messages", TOPIC_NAME)
                        .param("bootstrapServers", "yanlis:9092")
                        .param("limit", "5"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error", containsString("Topic mesajları alınamadı: " + TOPIC_NAME)))
                .andExpect(jsonPath("$.error", containsString("Kafka cluster'ına bağlanılamadı: yanlis:9092")));
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
            producer.send(new ProducerRecord<>(TOPIC_NAME, "order-4", "{\"orderId\": 4}")).get(5, TimeUnit.SECONDS);
            producer.send(new ProducerRecord<>(TOPIC_NAME, "order-5", "{\"orderId\": 5}")).get(5, TimeUnit.SECONDS);
            producer.send(new ProducerRecord<>(TOPIC_NAME, "order-6", "{\"orderId\": 6}")).get(5, TimeUnit.SECONDS);
            producer.flush();
        }
    }
}
