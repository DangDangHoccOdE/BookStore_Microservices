package com.bookstore.user.web.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException byId(String id) {
        return new UserNotFoundException("User not found with id: " + id);
    }

    public static UserNotFoundException byKeycloakId(String keycloakId) {
        return new UserNotFoundException("User not found with keycloakId: " + keycloakId);
    }
}
