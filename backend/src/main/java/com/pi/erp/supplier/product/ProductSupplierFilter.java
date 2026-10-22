package com.pi.erp.supplier.product;

import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ProductSupplierFilter(
    Long supplierId,
    Long productId,
    Long brandId,
    Long categoryId,
    String productName,
    String supplierProductCode,
    @PositiveOrZero BigDecimal minCost,
    @PositiveOrZero BigDecimal maxCost,
    @PositiveOrZero Integer avgDeliveryDays,
    Boolean preferred,
    Boolean active
) {}
