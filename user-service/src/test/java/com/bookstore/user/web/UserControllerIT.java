package com.bookstore.user.web;

import com.bookstore.user.AbstractIT;
import com.bookstore.user.domain.UserEntity;
import com.bookstore.user.domain.UserProfileEntity;
import com.bookstore.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class UserControllerIT extends AbstractIT {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void register_shouldCreateUserInKeyCloakAndDb() {
        String username = "user23";
        String email = "user123@example.com";

        Map<String, Object> request = Map.of(
                "email", email,
                "username", username,
                "password", "123123123@",
                "firstName", "string",
                "lastName", "string",
                "phone", "0123456789",
                "avatarUrl", "https://img.example.com/a.png"
        );

        webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.username").isEqualTo(username)
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.keycloakUserId").isNotEmpty();

        // DB verification
        var dbUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(dbUser.getUsername()).isEqualTo(username);

        // Keycloak verify
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(findRealmUserByUserName(username)).isPresent()
                );
    }

    @Test
    void register_shouldReturn409_whenDuplicateUserInKeyCloak() {
        String username = "dup_" + System.currentTimeMillis();
        String email = username+"@gmail.com";

        Map<String, Object> request = Map.of(
                "email", email,
                "username", username,
                "password", "Password123"
        );

        // first call OK
        webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // second call duplicate -> 409
        webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.title").isEqualTo("User Already Exists");

        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    @Test
    void register_shouldRollbackKeycloakUser_whenDbFails() {
        String conflictEmail = "abcd@gmail.com";
        String username = "user" + System.currentTimeMillis();

        UserEntity existing = new UserEntity();
        existing.setId(UUID.randomUUID());
        existing.setKeycloakUserId("keycloak-id" + UUID.randomUUID());
        existing.setUsername("existing" + System.currentTimeMillis());
        existing.setEmail(conflictEmail);
        existing.setUserStatus(UserStatus.ACTIVE);
        existing.setCreatedAt(OffsetDateTime.now());
        existing.setUpdatedAt(OffsetDateTime.now());
        userRepository.saveAndFlush(existing);

        Map<String, Object> request = Map.of(
                "email", conflictEmail,
                "username", username,
                "password", "Password123"
        );

        webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);

        assertThat(findRealmUserByUserName(username)).isEmpty();

        var users = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(conflictEmail))
                .toList();
        assertThat(users).hasSize(1);
    }

    @Test
    void me_shouldReturnCurrentUser_whenTokenValid() {
        var alice = findRealmUserByUserName("user12").orElseThrow(() -> new IllegalArgumentException("alice user not found in Keycloak"));

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setKeycloakUserId(alice.getId());
        user.setUsername("user12");
        user.setEmail("abcd2@gmail.com");
        user.setUserStatus(UserStatus.ACTIVE);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.saveAndFlush(user);

        UserProfileEntity profile = new UserProfileEntity();
        profile.setUser(user);
        profile.setFirstName("Alice");
        profile.setLastName("Hoang");
        profile.setPhone("0123123123");
        profile.setAvatarUrl("https://img.example.com/a.png");
        profile.setUpdatedAt(OffsetDateTime.now());
        userProfileRepository.saveAndFlush(profile);

        String token = getAccessToken("user12", "123");
        webTestClient.get()
                .uri("/api/users/me")
                .headers(h -> h.setBasicAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("user12")
                .jsonPath("$.profile.firstName").isEqualTo("Alice");
    }

    @Test
    void me_shouldReturn404_whenUserNotFoundInLocalDb() {
        String token = getAccessToken("alice", "password");

        webTestClient.get()
                .uri("/api/users/me")
                .headers(h -> h.setBasicAuth(token))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("User Not Found");
    }

    @Test
    void updateMe_shouldUpdateProfile() {
        var alice = findRealmUserByUserName("user12")
                .orElseThrow();

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setKeycloakUserId(alice.getId());
        user.setUsername("user12");
        user.setEmail("alice@gmail.com");
        user.setUserStatus(UserStatus.ACTIVE);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.saveAndFlush(user);

        String token = getAccessToken("user12", "123");

        Map<String, Object> request = Map.of(
                "firstName", "Alice",
                "lastName", "Hoang",
                "phone", "0123123123",
                "avatarUrl", "https://img.example.com/a.png"
        );

        webTestClient.put()
                .uri("/api/users/me")
                .headers(h -> h.setBasicAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.profile.firstName").isEqualTo("Alice")
                .jsonPath("$.profile.phone").isEqualTo("0123123123");
    }

    @Test
    void updateMe_shouldReturn401_whenNoToken() {
        webTestClient.put()
                .uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("firstName", "Alice"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void updateMe_shouldReturn404_whenUserNotFoundInLocalDb(){
        String token = getAccessToken("alice", "password");

        webTestClient.put()
                .uri("/api/users/me")
                .headers(h -> h.setBasicAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("firstName", "Alice"))
                .exchange()
                .expectStatus().isNotFound();
    }
}
