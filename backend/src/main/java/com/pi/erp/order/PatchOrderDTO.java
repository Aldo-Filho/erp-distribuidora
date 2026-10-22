package com.pi.erp.order;

import java.math.BigDecimal;

public record PatchOrderDTO(
    BigDecimal totalAmount,
    PaymentMethod paymentMethod,
    OrderStatus status,
    Long customerId,
    Long userId
) {}
