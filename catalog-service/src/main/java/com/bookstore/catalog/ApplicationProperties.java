package com.bookstore.catalog;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "services")
public record ApplicationProperties(
        @DefaultValue("10") @Min(1) int pageSize, String catalogServiceUrl, Keycloak keycloak) {

    public record Keycloak(String clientId) {}
}
