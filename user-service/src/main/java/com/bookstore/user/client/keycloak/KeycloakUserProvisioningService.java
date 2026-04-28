package com.bookstore.user.client.keycloak;

import com.bookstore.user.ApplicationProperties;
import com.bookstore.user.web.dto.RegisterUserRequest;
import com.bookstore.user.web.exception.KeycloakUserAlreadyExistsException;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KeycloakUserProvisioningService {
    private static final Logger log = LoggerFactory.getLogger(KeycloakUserProvisioningService.class);

    private final Keycloak keycloak;
    private final ApplicationProperties properties;

    public KeycloakUserProvisioningService(Keycloak keycloak, ApplicationProperties properties) {
        this.keycloak = keycloak;
        this.properties = properties;

        log.info("Keycloak config:");
        log.info("serverUrl = {}", properties.keycloak().serverUrl());
        log.info("realm = {}", properties.keycloak().realm());
        log.info("clientId = {}", properties.keycloak().clientId());
        log.info("clientSecret = {}", properties.keycloak().clientSecret());
    }

    @Retry(name = "keycloak")
    public String createUserWithDefaultRoles(RegisterUserRequest request) {
        var realm = keycloak.realm(properties.keycloak().realm());

        String userId = null;

        try {
            // 1. CREATE USER
            userId = createUser(request);

            // 2. ASSIGN ROLES
            assignDefaultRole(userId);

            return userId;

        } catch (Exception ex) {
            log.error("Provisioning failed → rollback Keycloak user: {}", userId, ex);

            if (userId != null) {
                try {
                    realm.users().delete(userId);
                    log.info("Rollback Keycloak user success: {}", userId);
                } catch (Exception deleteEx) {
                    log.error("Rollback Keycloak user FAILED: {}", userId, deleteEx);
                }
            }

            throw new IllegalStateException("User provisioning failed", ex);
        }
    }

    private String createUser(RegisterUserRequest request) {
        var realm = keycloak.realm(properties.keycloak().realm());

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setEnabled(true);
        user.setEmailVerified(true);

        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(request.password());
        password.setTemporary(false);
        user.setCredentials(List.of(password));

        try (Response response = realm.users().create(user)) {

            if (response.getStatus() == 201) {
                String location = response.getHeaderString("Location");
                return location.substring(location.lastIndexOf('/') + 1);
            }

            if (response.getStatus() == 409) {
                throw new KeycloakUserAlreadyExistsException("User already exists");
            }

            throw new RuntimeException("Failed to create user, status=" + response.getStatus());
        }
    }

    private void assignDefaultRole(String keycloakUserId) {
        try {
            var realm = keycloak.realm(properties.keycloak().realm());
            String defaultRole = properties.keycloak().defaultRole();
            String clientId = properties.keycloak().clientId();

            // Assign realm role
            RoleRepresentation userRole = realm.roles().get(defaultRole).toRepresentation();

            // Assign client role
            var client = realm.clients()
                    .findByClientId(clientId)
                    .stream()
                    .findFirst()
                    .orElseThrow();

            String clientUuid = client.getId();

            var clientRoles = realm.clients()
                    .get(clientUuid)
                    .roles();

            List<RoleRepresentation> rolesToAssign = List.of(
                    clientRoles.get("user.read").toRepresentation(),
                    clientRoles.get("user.update").toRepresentation()
            );

            if (userRole == null) {
                throw new IllegalStateException("Realm role not found: " + defaultRole);
            }

            realm.users().
                    get(keycloakUserId).
                    roles().realmLevel().
                    add(List.of(userRole));

            realm.users()
                    .get(keycloakUserId)
                    .roles()
                    .clientLevel(clientUuid)
                    .add(rolesToAssign);

            log.info("Default role {} assigned to user. userId={}", defaultRole, keycloakUserId);
        } catch (Exception ex) {
            log.error("Failed to assign default role to user. userId={}", keycloakUserId, ex);
            throw new IllegalStateException("Failed to assign default role", ex);
        }
    }

    @Retry(name = "keycloak")
    public void deleteUser(String keycloakUserId) {
        try {
            keycloak.realm(properties.keycloak().realm()).users().delete(keycloakUserId);
            log.info("User deleted from Keycloak. userId={}", keycloakUserId);
        } catch (Exception ex) {
            log.error("Failed to delete user from Keycloak. userId={}", keycloakUserId, ex);
            throw new IllegalStateException("Failed to delete user from Keycloak", ex);
        }
    }
}
