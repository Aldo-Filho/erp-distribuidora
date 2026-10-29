package com.pi.erp.purchase.item;

public record PurchaseItemFilter(
    Long purchaseId,
    Long productId,
    Long warehouseId
) {}
