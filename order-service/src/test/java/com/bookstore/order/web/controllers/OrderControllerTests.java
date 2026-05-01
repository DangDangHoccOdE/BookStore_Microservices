package com.bookstore.order.web.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import com.bookstore.order.AbstractIT;
import com.bookstore.order.domain.models.OrderSummary;
import com.bookstore.order.testdata.TestDataFactory;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

class OrderControllerTests extends AbstractIT {

    @Autowired
    private WebTestClient webTestClient;

    @Nested
    class CreateOrderTests {

        @Test
        void shouldCreateOrderSuccessfully() {
            mockGetProductByCode("P100", "Product 1", new BigDecimal("25.50"));
            String token = getAccessToken("usertest", "usertest");

            String payload =
                    """
                    {
                        "customer": {
                            "name": "dang",
                            "email": "haidang972004@gmail.com",
                            "phone": "999999999"
                        },
                        "deliveryAddress": {
                            "addressLine1": "HN",
                            "addressLine2": "HA Dong",
                            "city": "Hà Nội",
                            "state": "Ngô Quyền",
                            "zipCode": "10000",
                            "country": "VN"
                        },
                        "items": [
                            {
                                "code": "P100",
                                "name": "Product 1",
                                "price": 25.50,
                                "quantity": 1
                            }
                        ]
                    }
                    """;

            webTestClient
                    .post()
                    .uri("/api/orders")
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .exchange()
                    .expectStatus()
                    .isCreated()
                    .expectBody()
                    .jsonPath("$.orderNumber")
                    .isNotEmpty();
        }

        @Test
        void shouldReturnBadRequestWhenMandatoryDataIsMissing() {
            String token = getAccessToken("usertest", "usertest");
            var payload = TestDataFactory.createOrderRequestWithInvalidCustomer();

            webTestClient
                    .post()
                    .uri("/api/orders")
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .exchange()
                    .expectStatus()
                    .isBadRequest();
        }
    }

    @Nested
    @Sql(scripts = "classpath:/test-order.sql")
    class GetOrderTests {

        @Test
        void shouldGetOrderSuccessfully() {
            String token = getAccessToken("usertest", "usertest");

            List<OrderSummary> orders = webTestClient
                    .get()
                    .uri("/api/orders")
                    .headers(h -> h.setBearerAuth(token))
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBodyList(OrderSummary.class)
                    .returnResult()
                    .getResponseBody();

            assertThat(orders).hasSize(2);
        }
    }

    @Nested
    @Sql(scripts = "classpath:/test-order.sql")
    class GetOrderByOrderNumberTests {

        String orderNumber = "order-123";

        @Test
        void shouldGetOrderSuccessfully() {
            String token = getAccessToken("usertest", "usertest");

            webTestClient
                    .get()
                    .uri("/api/orders/{orderNumber}", orderNumber)
                    .headers(h -> h.setBearerAuth(token))
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody()
                    .jsonPath("$.orderNumber")
                    .isEqualTo(orderNumber);
        }
    }
}
