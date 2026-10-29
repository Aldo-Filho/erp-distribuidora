package com.pi.erp.stock.movement;

import jakarta.validation.constraints.NotNull;

public record RequestStockMovementDTO(
    @NotNull Long stockItemId,
    @NotNull StockMovementType type,
    @NotNull StockMovementReason reason,
    @NotNull Integer quantity,
    Long userId,
    String notes
) {}
