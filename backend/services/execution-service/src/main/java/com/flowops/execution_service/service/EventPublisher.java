package com.flowops.execution_service.service;

public interface EventPublisher {
    void publish(String topic, Object event, String key);
}
