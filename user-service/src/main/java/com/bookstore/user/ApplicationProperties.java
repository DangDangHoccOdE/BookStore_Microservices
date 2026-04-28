package com.bookstore.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "users")
public record ApplicationProperties(Keycloak keycloak, String eventsExchange) {
    public record Keycloak(String serverUrl, String realm, String clientId, String clientSecret, String defaultRole) {}
}
