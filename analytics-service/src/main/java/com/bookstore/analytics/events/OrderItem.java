package com.bookstore.analytics.events;

import java.math.BigDecimal;

public record OrderItem(String code, String name, BigDecimal price, int quantity) {}
