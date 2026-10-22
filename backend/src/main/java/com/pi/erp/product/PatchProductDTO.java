package com.pi.erp.product;

import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record PatchProductDTO(
    String name,
    Long brandId,
    Long categoryId,
    @PositiveOrZero BigDecimal cost,
    @PositiveOrZero BigDecimal price,
    @PositiveOrZero BigDecimal weightKg,
    String color,
    @PositiveOrZero BigDecimal dimensionX,
    @PositiveOrZero BigDecimal dimensionY,
    @PositiveOrZero BigDecimal dimensionZ,
    String size,
    Boolean active,
    Boolean removeCategory
) {}
