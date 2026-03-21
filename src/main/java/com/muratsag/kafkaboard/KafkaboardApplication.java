package com.muratsag.kafkaboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KafkaboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaboardApplication.class, args);
	}

}
