package com.bookstore.gateway.filter;

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationAndLoggingFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(CorrelationAndLoggingFilter.class);

    // Tạo request id để trace end-to-end
    // Gắn vào header để downstream service cũng log cùng id
    // Log path/status/duration
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = Optional.ofNullable(
                        exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"))
                .orElseGet(() -> {
                    String newId = UUID.randomUUID().toString();
                    log.info("Generated new Correlation ID: {}", newId);
                    return newId;
                });

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header("X-Correlation-Id", correlationId)
                .build();

        long start = System.currentTimeMillis();

        exchange.getResponse().getHeaders().add("X-Correlation-Id", correlationId);

        return chain.filter(exchange.mutate().request(request).build()).doOnSuccess(unused -> {
            long duration = System.currentTimeMillis() - start;
            log.info(
                    "cid={} method={} path={} status={} durationMs={}",
                    correlationId,
                    request.getMethod(),
                    request.getURI().getPath(),
                    exchange.getResponse().getStatusCode(),
                    duration);
        });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
