package com.bookstore.user;

import com.bookstore.user.domain.UserProfileRepository;
import com.bookstore.user.domain.UserRepository;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeEach;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Optional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
public abstract class AbstractIT {
    private static final String REALM = "bookstore";
    private static final String CLIENT_ID = "user-service-client";
    private static final String CLIENT_SECRET = "Su2ewBLfHmdmvFRE5qRruSu68oCgRHED";
    private static final String REQUIRED_AUDIENCE = "user-service-client";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("user_service_it")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final KeycloakContainer KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:26.3.0")
            .withRealmImportFile("/bookstore-realm.json")
            .withAdminUsername("admin_new")
            .withAdminPassword("123");

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserProfileRepository userProfileRepository;

//    @DynamicPropertySource
//    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
//        String issuerUri = KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM;
//
//        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
//        registry.add("spring.datasource.username", POSTGRES::getUsername);
//        registry.add("spring.datasource.password", POSTGRES::getPassword);
//
//        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuerUri);
//        registry.add("security.jwt.required-audience", () -> REQUIRED_AUDIENCE);
//
//        // Must match your ApplicationProperties(prefix = "users")
//        registry.add("users.keycloak.server-url", KEYCLOAK::getAuthServerUrl);
//        registry.add("users.keycloak.realm", () -> REALM);
//        registry.add("users.keycloak.client-id", () -> CLIENT_ID);
//        registry.add("users.keycloak.client-secret", () -> CLIENT_SECRET);
//        registry.add("users.keycloak.default-role", () -> "USER");
//    }

    @BeforeEach
    void cleanDatabase() {
        userProfileRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    protected String getAccessToken(String username, String password) {
        String tokenUrl = KEYCLOAK.getAuthServerUrl()
                + "/realms/" + REALM
                + "/protocol/openid-connect/token";

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

    protected Keycloak keyCloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK.getAuthServerUrl())
                .realm("master")
                .clientId("admin-cli")
                .grantType(OAuth2Constants.PASSWORD)
                .username(KEYCLOAK.getAdminUsername())
                .password(KEYCLOAK.getAdminPassword())
                .build();
    }

//    protected Optional<UserRepresentation> findRealmUserByUserName(String username) {
//        try (Keycloak kc = keyCloakAdminClient()) {
//            return kc.realm(REALM)
//                    .users()
//                    .searchByUsername(username, true)
//                    .stream().findFirst();
//        }
//    }

    protected Optional<UserRepresentation> findRealmUserByUserName(String username) {
        try (Keycloak kc = keyCloakAdminClient()) {
            return kc.realm(REALM)
                    .users()
                    .search(username)
                    .stream()
                    .filter(u -> username.equals(u.getUsername()))
                    .findFirst();
        }
    }
}
