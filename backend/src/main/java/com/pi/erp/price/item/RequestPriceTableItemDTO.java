package com.pi.erp.price.item;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequestPriceTableItemDTO(
    @NotNull Long priceTableId,
    @NotNull Long productId,
    @NotNull BigDecimal price
) {}
