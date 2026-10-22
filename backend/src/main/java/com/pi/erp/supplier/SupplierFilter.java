package com.pi.erp.supplier;

import jakarta.validation.constraints.PositiveOrZero;

public record SupplierFilter(
    Long supplierAddressId,
    String legalName,
    String tradeName,
    String taxId,
    String email,
    String phone,
    String whatsapp,
    @PositiveOrZero Integer avgDeliveryDays,
    Boolean active,
    Long productId,
    Long brandId,
    Long categoryId
) {}
