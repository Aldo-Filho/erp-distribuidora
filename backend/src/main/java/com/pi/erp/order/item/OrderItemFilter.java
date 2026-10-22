package com.pi.erp.order.item;

public record OrderItemFilter(
    Long orderId,
    Long productId
) {}
