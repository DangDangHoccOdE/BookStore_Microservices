package com.bookstore.catalog.config;

import com.bookstore.catalog.ApplicationProperties;
import java.util.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtAuthConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String clientId;

    public JwtAuthConverter(ApplicationProperties properties) {
        this.clientId = properties.keycloak().clientId();
    }

    private List<SimpleGrantedAuthority> extractRoles(Object rolesObj) {
        if (!(rolesObj instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .map(Object::toString)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new HashSet<>();

        // 1. Realm roles → ROLE_*
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            extractRoles(realmAccess.get("roles")).forEach(authorities::add);
        }

        // 2. Client roles → SCOPE_*
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            Object client = resourceAccess.get(clientId);
            if (client instanceof Map<?, ?> clientData) {
                Object roles = clientData.get("roles");
                if (roles instanceof Collection<?> list) {
                    list.forEach(role -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + role)));
                }
            }
        }

        // 3. Scope → SCOPE_*
        String scope = jwt.getClaimAsString("scope");
        if (scope != null) {
            Arrays.stream(scope.split(" "))
                    .filter(s -> !s.isBlank())
                    .forEach(s -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + s)));
        }

        return authorities;
    }
}
