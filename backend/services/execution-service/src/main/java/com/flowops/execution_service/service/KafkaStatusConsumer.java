package com.flowops.execution_service.service;

import com.flowops.execution_service.model.Run;
import com.flowops.execution_service.repository.RunRepository;
import com.flowops.kafka_contracts.Topics;
import com.flowops.kafka_contracts.events.FlowStatusEvent;
import com.flowops.kafka_contracts.events.StepStatusEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaStatusConsumer {

    private final RunRepository runRepository;
    private final EventStreamService eventStreamService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.EXECUTION_STATUS, groupId = "execution-service")
    public void consume(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.get("eventType").asText();

            switch (eventType) {
                case "FLOW_STATUS" -> {
                    FlowStatusEvent event = objectMapper.treeToValue(node, FlowStatusEvent.class);
                    handleFlowStatus(event);
                    eventStreamService.publishEvent(event);
                }
                case "STEP_STATUS" -> {
                    StepStatusEvent event = objectMapper.treeToValue(node, StepStatusEvent.class);
                    handleStepStatus(event);
                    eventStreamService.publishEvent(event);
                }
                default -> throw new IllegalArgumentException("Unknown eventType: " + eventType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse status event", e);
        }
    }

    private void handleFlowStatus(FlowStatusEvent event) {
        runRepository.findById(UUID.fromString(event.getRunId())).ifPresent(run -> {
            run.setStatus(Run.RunStatus.valueOf(event.getStatus().name()));
            run.setUpdatedAt(event.getTimestamp());
            runRepository.save(run);
        });
    }

    private void handleStepStatus(StepStatusEvent event) {
        runRepository.findById(UUID.fromString(event.getRunId())).ifPresent(run -> {
            run.getSteps().stream()
                .filter(step -> step.getStepId().equals(UUID.fromString(event.getStepId())))
                .findFirst()
                .ifPresent(step -> {
                    step.setStatus(Run.StepStatus.valueOf(event.getStatus().name()));
                    step.setError(event.getError());
                    step.setCompletedAt(event.getTimestamp());
                });
            run.setUpdatedAt(event.getTimestamp());
            runRepository.save(run);
        });
    }
}
