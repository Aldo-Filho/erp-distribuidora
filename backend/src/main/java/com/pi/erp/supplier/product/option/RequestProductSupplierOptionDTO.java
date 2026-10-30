package com.pi.erp.supplier.product.option;

import java.math.BigDecimal;

public record RequestProductSupplierOptionDTO(
    Long productSupplierId,
    String name,
    BigDecimal baseFactor,
    Boolean basePresentation,
    BigDecimal cost,
    Integer avgDeliveryDays,
    String supplierProductCode,
    Boolean active,
    Boolean confirmConversion
) {}
