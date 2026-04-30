package com.bookstore.user.config;

import com.bookstore.user.ApplicationProperties;
import java.util.concurrent.TimeUnit;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {
    @Bean
    Keycloak keycloakAdminClient(ApplicationProperties properties) {
        var keyCloak = properties.keycloak();
        return KeycloakBuilder.builder()
                .serverUrl(keyCloak.serverUrl())
                .realm(keyCloak.realm())
                .clientId(keyCloak.adminClientId())
                .clientSecret(keyCloak.adminClientSecret())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .resteasyClient(new ResteasyClientBuilderImpl()
                        .connectTimeout(3, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS)
                        .build())
                .build();
    }
}
