package com.pi.erp.supplier.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record RequestProductSupplierDTO(
    @NotNull Long productId,
    @NotNull Long supplierId,
    @NotNull @PositiveOrZero BigDecimal cost,
    @PositiveOrZero Integer avgDeliveryDays,
    String supplierProductCode,
    Boolean preferred,
    Boolean active
) {}
