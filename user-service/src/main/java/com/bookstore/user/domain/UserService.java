package com.bookstore.user.domain;

import com.bookstore.user.client.keycloak.KeycloakUserProvisioningService;
import com.bookstore.user.web.dto.RegisterUserRequest;
import com.bookstore.user.web.dto.RegisterUserResponse;
import com.bookstore.user.web.dto.UpdateUserProfileRequest;
import com.bookstore.user.web.dto.UserMeResponse;
import com.bookstore.user.web.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SecurityService securityService;
    private final KeycloakUserProvisioningService keycloakUserProvisioningService;

    public UserService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            SecurityService securityService,
            KeycloakUserProvisioningService keycloakUserProvisioningService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.securityService = securityService;
        this.keycloakUserProvisioningService = keycloakUserProvisioningService;
    }

    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        String keycloakUserId = keycloakUserProvisioningService.createUserWithDefaultRoles(request);

        try {
            UUID userId = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now();

            UserEntity user = new UserEntity();
            user.setId(userId);
            user.setKeycloakUserId(keycloakUserId);
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setUserStatus(UserStatus.ACTIVE);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            user = userRepository.saveAndFlush(user);

            UserProfileEntity profile = new UserProfileEntity();
            profile.setUser(user);
            profile.setFirstName(request.firstName());
            profile.setLastName(request.lastName());
            profile.setPhone(request.phone());
            profile.setAvatarUrl(request.avatarUrl());
            profile.setUpdatedAt(now);

            userProfileRepository.save(profile);

            return new RegisterUserResponse(
                    userId,
                    keycloakUserId,
                    user.getUsername(),
                    user.getEmail(),
                    user.getUserStatus().name());
        } catch (RuntimeException ex) {
            log.error("DB failed, rolling back Keycloak user: {}", keycloakUserId, ex);

            try {
                keycloakUserProvisioningService.deleteUser(keycloakUserId);
            } catch (Exception e) {
                log.error("Failed to delete Keycloak user", e);
            }

            throw ex;
        }
    }

    public UserMeResponse getCurrentUser() {
        String keycloakUserId = securityService.getCurrentSubject();

        UserEntity user = userRepository
                .findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new UserNotFoundException(keycloakUserId));

        UserProfileEntity profile = userProfileRepository.findById(user.getId()).orElse(null);

        return toResponse(user, profile);
    }

    @Transactional
    public UserMeResponse updateCurrentUser(UpdateUserProfileRequest request) {
        String keycloakUserId = securityService.getCurrentSubject();

        UserEntity user = userRepository
                .findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new UserNotFoundException(keycloakUserId));

        UserProfileEntity profile = userProfileRepository.findById(user.getId()).orElseGet(() -> {
            UserProfileEntity created = new UserProfileEntity();
            created.setUser(user);
            created.setUserId(user.getId());
            return created;
        });

        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhone(request.phone());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setUpdatedAt(OffsetDateTime.now());

        userProfileRepository.save(profile);
        return toResponse(user, profile);
    }

    private UserMeResponse toResponse(UserEntity user, UserProfileEntity profile) {
        UserMeResponse.Profile p = profile == null
                ? new UserMeResponse.Profile(null, null, null, null)
                : new UserMeResponse.Profile(
                        profile.getFirstName(), profile.getLastName(), profile.getPhone(), profile.getAvatarUrl());

        return new UserMeResponse(
                user.getId(),
                user.getKeycloakUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getUserStatus().name(),
                p);
    }
}
