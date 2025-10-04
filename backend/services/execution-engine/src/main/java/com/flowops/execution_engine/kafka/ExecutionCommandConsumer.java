package com.flowops.execution_engine.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowops.execution_engine.engine.ExecutionScheduler;
import com.flowops.kafka_contracts.Topics;
import com.flowops.kafka_contracts.events.ExecutionCommandEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ExecutionCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExecutionCommandConsumer.class);

    private final ObjectMapper objectMapper;
    private final ExecutionScheduler scheduler;

    public ExecutionCommandConsumer(ObjectMapper objectMapper, ExecutionScheduler scheduler) {
        this.objectMapper = objectMapper;
        this.scheduler = scheduler;
    }

    @KafkaListener(topics = Topics.EXECUTION_COMMANDS, groupId = "execution-engine")
    public void listen(ConsumerRecord<String, String> record) {
        try {
            String value = record.value();
            ExecutionCommandEvent cmd = objectMapper.readValue(value, ExecutionCommandEvent.class);

            log.info("Received ExecutionCommandEvent: type={} flowId={} runId={}",
                    cmd.getEventType(), cmd.getFlowId(), cmd.getRunId());

            switch (cmd.getEventType()) {
                case EXECUTION_START -> scheduler.startFlow(cmd);
                case EXECUTION_STOP -> scheduler.stopFlow(cmd.getFlowId(), cmd.getRunId());
                case EXECUTION_PAUSE -> scheduler.pauseFlow(cmd.getFlowId(), cmd.getRunId());
                case EXECUTION_RESUME -> scheduler.resumeFlow(cmd.getFlowId(), cmd.getRunId());
                default -> log.warn("Unknown eventType: {}", cmd.getEventType());
            }

        } catch (Exception e) {
            log.error("Failed to process ExecutionCommandEvent from Kafka", e);
        }
    }
}

