package com.bookstore.order.web.controllers;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

import com.bookstore.order.AbstractIT;
import com.bookstore.order.domain.models.OrderSummary;
import com.bookstore.order.testdata.TestDataFactory;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

class OrderControllerTests extends AbstractIT {

    @Nested
    class CreateOrderTests {
        @Test
        void shouldCreateOrderSuccessfully() {
            mockGetProductByCode("P100", "Product 1", new BigDecimal("25.50"));
            var payload =
                    """
                            {
                                "customer": {
                                    "name": "dang",
                                    "email": "haidang972004@gmail.com",
                                    "phone": "999999999"
                                },
                                "deliveryAddress": {
                                    "addressLine1": "HN",
                                    "addressLine2": "HA DOng",
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
            given().contentType(ContentType.JSON)
                    .body(payload)
                    .when()
                    .post("/api/orders")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("orderNumber", org.hamcrest.Matchers.notNullValue());
        }

        @Test
        void shouldReturnBadRequestWhenMandatoryDataIsMissing() {
            var payload = TestDataFactory.createOrderRequestWithInvalidCustomer();
            given().contentType(ContentType.JSON)
                    .body(payload)
                    .when()
                    .post("/api/orders")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value());
        }
    }

    @Nested
    @Sql(scripts = "classpath:/test-order.sql")
    class GetOrderTests {
        @Test
        void shouldGetOrderSuccessfully() {
            List<OrderSummary> orderSummaries = given().when()
                    .get("/api/orders")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .extract()
                    .body()
                    .as(new TypeRef<>() {});

            assertThat(orderSummaries).hasSize(2);
        }
    }

    @Nested
    @Sql(scripts = "classpath:/test-order.sql")
    class GetOrderByOrderNumberTests {
        String orderNumber = "order-123";

        @Test
        void shouldGetOrderSuccessfully() {
            given().when()
                    .get("/api/orders/{orderNumber}", orderNumber)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("orderNumber", is(orderNumber));
        }
    }
}
