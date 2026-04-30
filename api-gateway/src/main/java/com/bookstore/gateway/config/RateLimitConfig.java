package com.bookstore.gateway.config;

import java.security.Principal;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
    @Bean
    KeyResolver principalOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal().map(Principal::getName).switchIfEmpty(Mono.fromSupplier(() -> {
            var remote = exchange.getRequest().getRemoteAddress();
            return remote != null ? remote.getAddress().getHostAddress() : "anonymous";
        }));
    }

    //    Nếu có user login: rate limit theo user
    //    Nếu chưa login: rate limit theo IP
    //    Tránh 1 IP spam gateway
}
