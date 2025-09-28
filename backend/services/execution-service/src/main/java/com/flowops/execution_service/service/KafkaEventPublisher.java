package com.flowops.execution_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String topic, Object event, String key) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            // block to capture exceptions
            kafkaTemplate.send(topic, key, payload).get();
        } catch (Exception e) {
            // Log full stacktrace before wrapping
            e.printStackTrace();
            throw new RuntimeException("Failed to publish event to " + topic + ": " + e.getMessage(), e);
        }
    }
}
