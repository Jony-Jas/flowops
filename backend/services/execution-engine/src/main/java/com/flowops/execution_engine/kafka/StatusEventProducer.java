package com.flowops.execution_engine.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.kafka_contracts.events.StepStatusEvent;
import com.flowops.kafka_contracts.Topics;
import com.flowops.kafka_contracts.events.FlowStatusEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Producer for publishing step and flow status events to Kafka.
 * We communicate as <String, String> (key = flowId:runId, value = JSON string).
 */
@Component
public class StatusEventProducer {

    private static final Logger log = LoggerFactory.getLogger(StatusEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private final String stepTopic = Topics.EXECUTION_STATUS;
    private final String flowTopic = Topics.EXECUTION_STATUS;

    public StatusEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendStepStatus(String flowId, String runId, String stepId,
                               StepStatusEvent.Status status, String error) {
        StepStatusEvent ev = new StepStatusEvent();
        ev.setEventType(StepStatusEvent.EventType.STEP_STATUS);
        ev.setFlowId(flowId);
        ev.setRunId(runId);
        ev.setStepId(stepId);
        ev.setStatus(status);
        ev.setError(error);
        ev.setTimestamp(Instant.now());

        sendJson(stepTopic, flowId + ":" + runId, ev);
    }

    public void sendFlowStatus(String flowId, String runId,
                               FlowStatusEvent.Status status, String error) {
        FlowStatusEvent ev = new FlowStatusEvent();
        ev.setEventType(FlowStatusEvent.EventType.FLOW_STATUS);
        ev.setFlowId(flowId);
        ev.setRunId(runId);
        ev.setStatus(status);
        ev.setError(error);
        ev.setTimestamp(Instant.now());

        sendJson(flowTopic, flowId + ":" + runId, ev);
    }

    private void sendJson(String topic, String key, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(new ProducerRecord<>(topic, key, json));
            log.debug("Published to {} key={} payload={}", topic, key, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for topic {}: {}", topic, e.getMessage(), e);
        }
    }
}

