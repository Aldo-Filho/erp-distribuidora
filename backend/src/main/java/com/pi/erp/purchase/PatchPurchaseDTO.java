package com.pi.erp.purchase;

import java.time.LocalDate;

public record PatchPurchaseDTO(
    Long supplierId,
    Long userId,
    LocalDate expectedDeliveryDate,
    Boolean removeExpectedDeliveryDate,
    PurchaseStatus status
) {}
