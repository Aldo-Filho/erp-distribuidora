package com.pi.erp.order.item;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequestOrderItemDTO(
    @NotNull Long orderId,
    @NotNull Long productId,
    @NotNull Integer quantity,
    @NotNull BigDecimal unitPrice,
    BigDecimal discount
) {}
