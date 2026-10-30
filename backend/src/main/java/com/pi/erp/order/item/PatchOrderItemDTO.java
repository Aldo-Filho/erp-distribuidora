package com.pi.erp.order.item;

import java.math.BigDecimal;

public record PatchOrderItemDTO(
    Long orderId,
    Long productId,
    Long warehouseId,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal discount,
    DiscountType discountType,
    Long presentationId,
    Boolean removePresentation
) {}
