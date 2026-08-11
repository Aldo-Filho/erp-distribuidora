package com.pi.erp.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RequestProductDTO(
        @NotBlank
        String name,

        // Ajustar SKU para geração automática
        @NotBlank
        String sku,

        @NotNull
        Long brandId,

        Long categoryId,

        @NotNull
        BigDecimal cost,

        @NotNull
        BigDecimal price,

        BigDecimal weightKg,

        String color,

        BigDecimal dimensionX,
        BigDecimal dimensionY,
        BigDecimal dimensionZ

) {
}
