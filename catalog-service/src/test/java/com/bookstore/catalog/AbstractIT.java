package com.bookstore.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Testcontainers;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureWebTestClient
public abstract class AbstractIT {
    private static final String REALM = "bookstore";
    private static final String CLIENT_ID = "user-service-admin";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String REQUIRED_AUDIENCE = "catalog-service-api";

    @LocalServerPort
    int port;

    static final KeycloakContainer KEYCLOAK = SharedKeycloak.INSTANCE;

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        String issuer = KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM;

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuer);
        registry.add(
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> issuer + "/protocol/openid-connect/certs");
        registry.add("security.jwt.required-audience", () -> REQUIRED_AUDIENCE);
        registry.add("services.keycloak.client-id", () -> CLIENT_ID);
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    protected String getAccessToken(String username, String password) throws JsonProcessingException {
        String tokenUrl = KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", CLIENT_ID);
        form.add("client_secret", CLIENT_SECRET);
        form.add("username", username);
        form.add("password", password);

        String response = WebClient.builder()
                .baseUrl(tokenUrl)
                .build()
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(status -> status.isError(), res -> res.bodyToMono(String.class)
                        .map(body -> new RuntimeException("Keycloak error: " + body)))
                .bodyToMono(String.class)
                .block();

        Map<String, Object> tokenResponse = new ObjectMapper().readValue(response, Map.class);
        if (tokenResponse == null || tokenResponse.get("access_token") == null) {
            throw new IllegalStateException("Could not obtain access token from Keycloak");
        }

        return tokenResponse.get("access_token").toString();
    }
}
