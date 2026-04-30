package com.bookstore.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIT {
    private static final String REALM = "bookstore";
    private static final String CLIENT_ID = "user-service-admin";
    private static final String CLIENT_SECRET = "Su2ewBLfHmdmvFRE5qRruSu68oCgRHED";
    private static final String REQUIRED_AUDIENCE = "catalog-service-api";

    @LocalServerPort
    int port;

    @Container
    static final KeycloakContainer KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.3.0")
            .withRealmImportFile("/bookstore-realm.json")
            .withAdminUsername("admin_new")
            .withAdminPassword("123");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        String issuerUri = KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM;

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuerUri);
        registry.add("security.jwt.required-audience", () -> REQUIRED_AUDIENCE);
        registry.add("services.keycloak.client-id", () -> CLIENT_ID);
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    protected String getAccessToken(String username, String password) {
        String tokenUrl = KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM + "/protocol/openid-connect/token";

        String body = "grant_type=password&client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET + "&username="
                + username + "&password=" + password;

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Failed to get token: " + response.statusCode());
            }

            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = mapper.readValue(response.body(), Map.class);
            if (!responseBody.containsKey("access_token")) {
                throw new IllegalStateException("Could not obtain access token from Keycloak");
            }
            return responseBody.get("access_token").toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get access token", e);
        }
    }
}
