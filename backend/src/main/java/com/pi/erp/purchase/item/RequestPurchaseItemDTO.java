package com.pi.erp.purchase.item;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequestPurchaseItemDTO(
    @NotNull Long purchaseId,
    @NotNull Long productId,
    @NotNull Long warehouseId,
    @NotNull Integer quantity,
    BigDecimal unitCost
) {}
