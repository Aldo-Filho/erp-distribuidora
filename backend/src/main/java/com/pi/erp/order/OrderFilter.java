package com.pi.erp.order;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public record OrderFilter(
    PaymentMethod paymentMethod,
    OrderStatus status,
    Long customerId,
    Long userId,
    Long number,
    String query,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo
) {
    public OrderFilter(
        PaymentMethod paymentMethod,
        OrderStatus status,
        Long customerId,
        Long userId
    ) {
        this(paymentMethod, status, customerId, userId, null, null, null, null);
    }
}
