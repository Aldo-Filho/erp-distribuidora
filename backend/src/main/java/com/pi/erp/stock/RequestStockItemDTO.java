package com.pi.erp.stock;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequestStockItemDTO(
    @NotNull Long warehouseId,
    @NotNull Long productId,
    @NotNull BigDecimal quantity,
    @NotNull BigDecimal reservedQuantity,
    @NotNull BigDecimal minQuantity,
    BigDecimal maxQuantity
) {}
