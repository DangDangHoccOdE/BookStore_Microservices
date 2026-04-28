package com.bookstore.user.domain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {
    public String getCurrentSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new IllegalStateException("Authentication is not JWT: " + authentication.getClass());
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
        Jwt jwt = (Jwt) jwtAuth.getPrincipal();
        return jwt.getSubject();
    }
}
