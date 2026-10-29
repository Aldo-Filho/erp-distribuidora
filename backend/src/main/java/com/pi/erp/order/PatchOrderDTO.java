package com.pi.erp.order;

public record PatchOrderDTO(
    PaymentMethod paymentMethod,
    OrderStatus status,
    Long customerId,
    Long userId
) {}
