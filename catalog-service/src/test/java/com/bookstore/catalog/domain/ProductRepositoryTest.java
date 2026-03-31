package com.bookstore.catalog.domain;

import java.math.BigDecimal;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest(
        properties = {
            "spring.test.database.replace=none",
            "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///db",
        })
// @Import(TestcontainersConfiguration.class)
@Sql("/test-data.sql")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Test
    void shouldGetAllProducts() {
        List<ProductEntity> products = repository.findAll();
        Assertions.assertThat(products).hasSize(15);
    }

    @Test
    void shouldGetProductByCode() {
        ProductEntity product = repository.findByCode("P100").orElseThrow();
        Assertions.assertThat(product.getCode()).isEqualTo("P100");
        Assertions.assertThat(product.getName()).isEqualTo("The Hunger Games");
        Assertions.assertThat(product.getDescription())
                .isEqualTo("Winning will make you famous. Losing means certain death...");
        Assertions.assertThat(product.getPrice()).isEqualTo(new BigDecimal("34.0"));
    }

    @Test
    void shouldReturnEmptyWhenProductCodeNotExists() {
        Assertions.assertThat(repository.findByCode("invalid")).isEmpty();
    }
}
