package com.bookstore.user.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 30) String phone,
        @Size(max = 255) String avatarUrl) {}
