package com.bookstore.catalog.web.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.catalog.AbstractIT;
import com.bookstore.catalog.domain.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

@Sql("/test-data.sql")
class ProductControllerTest extends AbstractIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnProducts() throws JsonProcessingException {
        String token = getAccessToken("usertest", "usertest");

        webTestClient
                .get()
                .uri("/api/products")
                .headers(h -> h.setBearerAuth(token)) // ✅ QUAN TRỌNG
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.data.length()")
                .isEqualTo(10)
                .jsonPath("$.totalElements")
                .isEqualTo(15)
                .jsonPath("$.pageNumber")
                .isEqualTo(1)
                .jsonPath("$.totalPages")
                .isEqualTo(2)
                .jsonPath("$.isFirst")
                .isEqualTo(true)
                .jsonPath("$.isLast")
                .isEqualTo(false)
                .jsonPath("$.hasNext")
                .isEqualTo(true)
                .jsonPath("$.hasPrevious")
                .isEqualTo(false);
    }

    @Test
    void shouldGetProductByCode() throws JsonProcessingException {
        String token = getAccessToken("usertest", "usertest");

        Product product = webTestClient
                .get()
                .uri("/api/products/{code}", "P100")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Product.class)
                .returnResult()
                .getResponseBody();

        assertThat(product).isNotNull();
        assertThat(product.code()).isEqualTo("P100");
        assertThat(product.name()).isEqualTo("The Hunger Games");
        assertThat(product.description()).isEqualTo("Winning will make you famous. Losing means certain death...");
        assertThat(product.price()).isEqualTo(new BigDecimal("34.0"));
    }

    @Test
    void shouldReturnNotFoundWhenProductCodeNotExists() throws JsonProcessingException {
        String code = "invalid_product_code";
        String token = getAccessToken("usertest", "usertest");

        webTestClient
                .get()
                .uri("/api/products/{code}", code)
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(404)
                .jsonPath("$.title")
                .isEqualTo("Product Not Found")
                .jsonPath("$.detail")
                .isEqualTo("Product not found with code: " + code);
    }
}
