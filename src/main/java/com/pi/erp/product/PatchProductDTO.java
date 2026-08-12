package com.pi.erp.product;

import java.math.BigDecimal;

public record PatchProductDTO(
        String name,
        Long brandId,
        Long categoryId,
        BigDecimal cost,
        BigDecimal price,
        BigDecimal weightKg,
        String color,
        BigDecimal dimensionX,
        BigDecimal dimensionY,
        BigDecimal dimensionZ,
        String size
) {
}
