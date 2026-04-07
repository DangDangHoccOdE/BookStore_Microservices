package com.bookstore.order.testdata;

import static org.instancio.Select.field;

import com.bookstore.order.domain.models.Address;
import com.bookstore.order.domain.models.CreatedOrderRequest;
import com.bookstore.order.domain.models.Customer;
import com.bookstore.order.domain.models.OrderItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.instancio.Instancio;

public class TestDataFactory {
    static final List<String> VALID_COUNTIES = List.of("India", "Germany", "VN");
    static final Set<OrderItem> VALID_ORDER_ITEMS =
            Set.of(new OrderItem("P100", "Product 1", new BigDecimal("25.50"), 1));
    static final Set<OrderItem> INVALID_ORDER_ITEMS =
            Set.of(new OrderItem("ABCD", "Product 1", new BigDecimal("25.50"), 1));

    public static CreatedOrderRequest createValidOrderRequest() {
        return Instancio.of(CreatedOrderRequest.class)
                .generate(field(Customer::email), gen -> gen.text().pattern("#a#a#a#a#a#a@mail.com"))
                .set(field(CreatedOrderRequest::items), VALID_ORDER_ITEMS)
                .generate(field(Address::country), gen -> gen.oneOf(VALID_COUNTIES))
                .create();
    }

    public static CreatedOrderRequest createOrderRequestWithInvalidCustomer() {
        return Instancio.of(CreatedOrderRequest.class)
                .generate(field(Customer::email), gen -> gen.text().pattern("#c#c#c#c#d#d@mail.com"))
                .set(field(Customer::phone), "")
                .generate(field(Address::country), gen -> gen.oneOf(VALID_COUNTIES))
                .set(field(CreatedOrderRequest::items), VALID_ORDER_ITEMS)
                .create();
    }

    public static CreatedOrderRequest createOrderRequestWithInvalidDeliveryAddress() {
        return Instancio.of(CreatedOrderRequest.class)
                .generate(field(Customer::email), gen -> gen.text().pattern("#c#c#c#c#d#d@mail.com"))
                .set(field(Address::country), "")
                .set(field(CreatedOrderRequest::items), VALID_ORDER_ITEMS)
                .create();
    }

    public static CreatedOrderRequest createOrderRequestWithNoItems() {
        return Instancio.of(CreatedOrderRequest.class)
                .generate(field(Customer::email), gen -> gen.text().pattern("#c#c#c#c#d#d@mail.com"))
                .generate(field(Address::country), gen -> gen.oneOf(VALID_COUNTIES))
                .set(field(CreatedOrderRequest::items), Set.of())
                .create();
    }
}
