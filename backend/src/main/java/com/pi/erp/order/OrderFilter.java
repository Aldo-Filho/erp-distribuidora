package com.pi.erp.order;

public record OrderFilter(
    PaymentMethod paymentMethod,
    OrderStatus status,
    Long customerId,
    Long userId
) {}
