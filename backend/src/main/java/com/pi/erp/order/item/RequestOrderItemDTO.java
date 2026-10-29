package com.pi.erp.order.item;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequestOrderItemDTO(
    @NotNull Long orderId,
    @NotNull Long productId,
    @NotNull Long warehouseId,
    @NotNull Integer quantity,
    BigDecimal unitPrice,
    BigDecimal discount,
    DiscountType discountType
) {}
