package com.pi.erp.purchase.item;

import java.math.BigDecimal;

public record PatchPurchaseItemDTO(
    Long productId,
    Long warehouseId,
    BigDecimal quantity,
    BigDecimal unitCost,
    Long supplierOptionId
) {}
