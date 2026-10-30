package com.pi.erp.purchase.item;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequestPurchaseItemDTO(
    @NotNull Long purchaseId,
    @NotNull Long productId,
    @NotNull Long warehouseId,
    @NotNull BigDecimal quantity,
    BigDecimal unitCost,
    Long supplierOptionId
) {}
