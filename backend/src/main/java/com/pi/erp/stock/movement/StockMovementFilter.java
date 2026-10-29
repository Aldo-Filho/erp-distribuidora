package com.pi.erp.stock.movement;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public record StockMovementFilter(
    Long stockItemId,
    Long warehouseId,
    Long productId,
    StockMovementType type,
    StockMovementReason reason,
    Long orderId,
    Long purchaseId,
    Long userId,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime occurredTo
) {}
