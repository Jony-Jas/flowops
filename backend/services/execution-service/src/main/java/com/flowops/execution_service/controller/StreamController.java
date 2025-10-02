package com.flowops.execution_service.controller;

import com.flowops.execution_service.service.EventStreamService;
import com.flowops.shared_api.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/executions/stream")
@RequiredArgsConstructor
public class StreamController {

    private final EventStreamService eventStreamService;

    @GetMapping(value = "/{runId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ApiResponse<?>>> streamRunEvents(@PathVariable UUID runId) {
    return eventStreamService.subscribe(runId) // Flux<Object>
        .map(event -> {
                return ServerSentEvent.<ApiResponse<?>>builder(ApiResponse.success(event)).build();
        });
    }
}
