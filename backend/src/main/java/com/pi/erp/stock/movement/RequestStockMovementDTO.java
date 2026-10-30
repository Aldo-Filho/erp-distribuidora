package com.pi.erp.stock.movement;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record RequestStockMovementDTO(
    @NotNull UUID requestId,
    @NotNull Long stockItemId,
    @NotNull StockMovementType type,
    @NotNull StockMovementReason reason,
    @NotNull BigDecimal quantity,
    Long userId,
    String notes
) {}
