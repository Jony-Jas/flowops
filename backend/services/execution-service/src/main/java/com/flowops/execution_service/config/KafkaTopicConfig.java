package com.flowops.execution_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.flowops.kafka_contracts.Topics;

@Configuration
@Profile("dev") // only active in dev profile
public class KafkaTopicConfig {

    @Bean
    public NewTopic executionCommandsTopic() {
        return new NewTopic(Topics.EXECUTION_COMMANDS, 3, (short) 1);
    }

    @Bean
    public NewTopic executionStatusTopic() {
        return new NewTopic(Topics.EXECUTION_STATUS, 3, (short) 1);
    }
}
