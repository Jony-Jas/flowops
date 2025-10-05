package com.flowops.api_gateway.filter;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

    @Value("${auth.verify-url:http://localhost:8084/auth/verify}")
    private String authVerifyUrl;

    public JwtAuthFilter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip authentication for login and public endpoints
        if (path.startsWith("/auth")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        // Verify token with auth-service
        return webClient.post()
                .uri(authVerifyUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"token\": \"" + token + "\"}")
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(),
                        response -> Mono.error(new RuntimeException("Invalid token")))
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    Boolean valid = (Boolean) response.get("valid");
                    String subject = (String) response.get("subject");

                    if (Boolean.TRUE.equals(valid) && subject != null) {
                        // Inject X-User-Id header
                        ServerWebExchange mutatedExchange = exchange.mutate()
                                .request(r -> r.headers(headers -> headers.add("X-User-Id", subject)))
                                .build();

                        return chain.filter(mutatedExchange);
                    } else {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                })
                .onErrorResume(error -> {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
