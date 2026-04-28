package com.bookstore.user.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(security = @SecurityRequirement(name = "keycloak"))
@SecurityScheme(
        name = "keycloak",
        type = SecuritySchemeType.OAUTH2,
        flows =
                @OAuthFlows(
                        password =
                                @OAuthFlow(
                                        tokenUrl =
                                                "http://localhost:9191/realms/bookstore/protocol/openid-connect/token"),
                        clientCredentials =
                                @OAuthFlow(
                                        tokenUrl =
                                                "http://localhost:9191/realms/bookstore/protocol/openid-connect/token")))
@Configuration
public class OpenApiConfig {}
