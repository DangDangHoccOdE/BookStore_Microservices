package com.bookstore.order;

import dasniko.testcontainers.keycloak.KeycloakContainer;

public final class SharedKeycloak {

    public static final KeycloakContainer INSTANCE = new KeycloakContainer("quay.io/keycloak/keycloak:26.3.0")
            .withRealmImportFile("/bookstore-realm.json")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withStartupTimeout(java.time.Duration.ofMinutes(2));

    static {
        INSTANCE.start(); // started once, JVM shutdown hook stops it
    }

    private SharedKeycloak() {}
}
