package com.pi.erp.product.presentation;

import java.math.BigDecimal;

public record PatchProductPresentationDTO(
    String name,
    BigDecimal baseFactor,
    BigDecimal price,
    Boolean active,
    Boolean removePrice
) {}
