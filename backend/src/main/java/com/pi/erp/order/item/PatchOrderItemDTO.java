package com.pi.erp.order.item;

import java.math.BigDecimal;

public record PatchOrderItemDTO(
    Long orderId,
    Long productId,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal discount
) {}
