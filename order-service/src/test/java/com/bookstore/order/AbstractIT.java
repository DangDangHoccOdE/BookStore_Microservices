package com.bookstore.order;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.client.WireMock;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.restassured.RestAssured;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
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
import org.wiremock.integrations.testcontainers.WireMockContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Testcontainers
@AutoConfigureWebTestClient
public abstract class AbstractIT {
    private static final String REALM = "bookstore";
    private static final String CLIENT_ID = "user-service-admin";
    private static final String CLIENT_SECRET = "test-secret";
    private static final String REQUIRED_AUDIENCE = "order-service-api";

    @LocalServerPort
    int port;

    static final KeycloakContainer KEYCLOAK = SharedKeycloak.INSTANCE;

    static {
        // Đảm bảo Keycloak đã start xong trước khi
        // @DynamicPropertySource chạy và Spring context được tạo
        if (!KEYCLOAK.isRunning()) {
            KEYCLOAK.start();
        }
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        String issuer = KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM;

        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuer);
        //        registry.add(
        //                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        //                () -> issuer + "/protocol/openid-connect/certs");
        registry.add("security.jwt.required-audience", () -> REQUIRED_AUDIENCE);
        registry.add("services.keycloak.client-id", () -> CLIENT_ID);

        registry.add(
                "orders.catalog-service-url",
                () -> "http://" + wireMockContainer.getHost() + ":" + wireMockContainer.getPort());
    }

    static WireMockContainer wireMockContainer = new WireMockContainer("wiremock/wiremock:3.5.2-alpine");

    @BeforeAll
    static void beforeAll() {
        wireMockContainer.start();
        configureFor(wireMockContainer.getHost(), wireMockContainer.getPort());
    }

    @BeforeEach
    public void setup() {
        RestAssured.port = port;
    }

    protected String getAccessToken(String username, String password) {
        String tokenUrl = KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", CLIENT_ID);
        form.add("client_secret", CLIENT_SECRET);
        form.add("username", username);
        form.add("password", password);

        Map<String, Object> tokenResponse = WebClient.builder()
                .baseUrl(tokenUrl)
                .build()
                .post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (tokenResponse == null || tokenResponse.get("access_token") == null) {
            throw new IllegalStateException("Could not obtain access token from Keycloak");
        }

        return tokenResponse.get("access_token").toString();
    }

    protected static void mockGetProductByCode(String code, String name, BigDecimal price) {
        stubFor(WireMock.get(urlMatching("/api/products/" + code))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withStatus(200)
                        .withBody(
                                """
                                {
                                    "code": "%s",
                                    "name": "%s",
                                    "price": %f
                                }
                                """
                                        .formatted(code, name, price.doubleValue()))));
    }
}
