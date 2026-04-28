package com.bookstore.user.web.dto;

import java.util.UUID;

public record RegisterUserResponse(UUID userId, String keycloakUserId, String username, String email, String status) {}
