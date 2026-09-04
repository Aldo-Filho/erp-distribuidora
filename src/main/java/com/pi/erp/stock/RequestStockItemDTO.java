package com.pi.erp.stock;

import jakarta.validation.constraints.NotNull;

public record RequestStockItemDTO(
        @NotNull
        Long warehouseId,
        @NotNull
        Long productId,
        @NotNull
        Integer quantity,
        @NotNull
        Integer reservedQuantity,
        @NotNull
        Integer minQuantity,
        @NotNull
        Integer maxQuantity
) {
}
