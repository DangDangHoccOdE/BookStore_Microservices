package com.bookstore.user.web.exception;

public class KeycloakUserAlreadyExistsException extends RuntimeException {
    public KeycloakUserAlreadyExistsException(String message) {
        super(message);
    }
}
