package com.bookstore.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {
    // Giảm rủi ro clickjacking
    // Giảm cache dữ liệu nhạy cảm
    // Tăng hardening phía edge
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
        exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
        exchange.getResponse().getHeaders().add("Referrer-Policy", "no-referrer");
        exchange.getResponse().getHeaders().add("Cache-Control", "no-store");
        exchange.getResponse().getHeaders().add("Content-Security-Policy", "default-src 'self'");
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
