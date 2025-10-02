package com.flowops.execution_service.service;

import com.flowops.kafka_contracts.events.FlowStatusEvent;
import com.flowops.kafka_contracts.events.StepStatusEvent;
import org.springframework.stereotype.Service;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventStreamService {

    private final Sinks.Many<Object> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final Map<UUID, Disposable> subscriptions = new ConcurrentHashMap<>();

    // Called by Kafka consumer to push new events
    public void publishEvent(Object event) {
        sink.tryEmitNext(event);
    }

    // SSE subscription filtered by runId
    public Flux<Object> subscribe(UUID runId) {
       Flux<Object> flux = sink.asFlux()
                .filter(event -> {
                    if (event instanceof FlowStatusEvent flowEvent) {
                        return flowEvent.getRunId().equals(runId.toString());
                    } else if (event instanceof StepStatusEvent stepEvent) {
                        return stepEvent.getRunId().equals(runId.toString());
                    }
                    return false;
                })
                .doOnCancel(() -> cleanup(runId))   // cleanup when client disconnects
                .doOnTerminate(() -> cleanup(runId));

        // Track subscription so we can cancel manually if needed
        Disposable disposable = flux.subscribe();
        subscriptions.put(runId, disposable);

        return flux;
    }

    private void cleanup(UUID runId) {
        Disposable disposable = subscriptions.remove(runId);
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
