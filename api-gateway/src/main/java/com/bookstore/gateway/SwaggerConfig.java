package com.bookstore.gateway;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Configuration;

@Configuration
class SwaggerConfig {

    private final RouteDefinitionLocator locator;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    SwaggerConfig(RouteDefinitionLocator locator, SwaggerUiConfigProperties swaggerUiConfigProperties) {
        this.locator = locator;
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
    }

    @PostConstruct
    public void init() {
        List<RouteDefinition> definitions =
                locator.getRouteDefinitions().collectList().block();

        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = new HashSet<>();

        definitions.stream().filter(route -> route.getId().endsWith("-service")).forEach(route -> {
            String serviceName = route.getId().replace("-service", "");

            urls.add(new AbstractSwaggerUiConfigProperties.SwaggerUrl(
                    serviceName, "/" + serviceName + "/v3/api-docs", serviceName));
        });

        swaggerUiConfigProperties.setUrls(urls);
    }
}
