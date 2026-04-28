package com.bookstore.user.web.dto;

import java.util.UUID;

public record UserMeResponse(
        UUID userId, String keycloakUserId, String username, String email, String status, Profile profile) {

    public record Profile(String firstName, String lastName, String phone, String avatarUrl) {}
}
