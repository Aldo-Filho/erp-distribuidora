package com.pi.erp.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

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
        BigDecimal dimensionZ,
        String size,

        @Valid
        InitialStockDTO stock
) {
        public record InitialStockDTO(
                @NotNull
                @Positive
                Long warehouseId,
                @NotNull
                @PositiveOrZero
                Integer quantity,
                @NotNull
                @PositiveOrZero
                Integer reservedQuantity,
                @NotNull
                @PositiveOrZero
                Integer minQuantity,
                @NotNull
                @PositiveOrZero
                Integer maxQuantity
        ) {
        }

}
