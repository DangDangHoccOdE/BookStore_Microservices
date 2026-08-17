package com.bookstore.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AnalyticsServiceApplicationTests extends AbstractIT {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {}
}
