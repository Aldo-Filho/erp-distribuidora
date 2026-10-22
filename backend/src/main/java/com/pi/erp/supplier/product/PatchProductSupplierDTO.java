package com.pi.erp.supplier.product;

import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record PatchProductSupplierDTO(
    Long productId,
    Long supplierId,
    @PositiveOrZero BigDecimal cost,
    @PositiveOrZero Integer avgDeliveryDays,
    String supplierProductCode,
    Boolean preferred,
    Boolean active
) {}
