package com.bookstore.user.web.controller;

import com.bookstore.user.domain.UserService;
import com.bookstore.user.web.dto.RegisterUserRequest;
import com.bookstore.user.web.dto.RegisterUserResponse;
import com.bookstore.user.web.dto.UpdateUserProfileRequest;
import com.bookstore.user.web.dto.UserMeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterUserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return userService.register(request);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('SCOPE_user.read')")
    public UserMeResponse me() {
        return userService.getCurrentUser();
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('SCOPE_user.update')")
    public UserMeResponse updateMe(@Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateCurrentUser(request);
    }
}
