package com.bookstore.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
public record ApplicationProperties(Keycloak keycloak) {

    public record Keycloak(String clientId) {}
}
