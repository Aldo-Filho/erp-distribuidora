package com.pi.erp.supplier.product.option;

import java.math.BigDecimal;

public record PatchProductSupplierOptionDTO(
    String name,
    BigDecimal baseFactor,
    Boolean basePresentation,
    BigDecimal cost,
    Integer avgDeliveryDays,
    String supplierProductCode,
    Boolean active,
    Boolean confirmConversion,
    Boolean removeAvgDeliveryDays
) {}
