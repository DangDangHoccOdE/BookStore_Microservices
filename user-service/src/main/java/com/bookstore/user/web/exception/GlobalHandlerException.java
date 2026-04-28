package com.bookstore.user.web.exception;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalHandlerException extends ResponseEntityExceptionHandler {
    private static final String SERVICE_NAME = "user-service";

    private static final URI NOT_FOUND_TYPE = URI.create("https://api.bookstore.com/errors/not-found");
    private static final URI CONFLICT_TYPE = URI.create("https://api.bookstore.com/errors/conflict");

    @ExceptionHandler(KeycloakUserAlreadyExistsException.class)
    ProblemDetail handleKeycloakUserAlreadyExists(KeycloakUserAlreadyExistsException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());

        problemDetail.setTitle("User Already Exists");
        problemDetail.setType(CONFLICT_TYPE);
        problemDetail.setProperty("service", SERVICE_NAME);
        problemDetail.setProperty("error_category", "Business");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleUserNotFoundException(UserNotFoundException e) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "User not found");

        problemDetail.setTitle("User Not Found");
        problemDetail.setType(NOT_FOUND_TYPE);
        problemDetail.setProperty("service", SERVICE_NAME);
        problemDetail.setProperty("error_category", "Business");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }
}
