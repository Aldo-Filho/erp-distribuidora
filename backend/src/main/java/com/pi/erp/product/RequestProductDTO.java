package com.pi.erp.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record RequestProductDTO(
    @NotBlank String name,
    // TODO: Generate the SKU automatically.
    @NotBlank String sku,
    @NotNull Long brandId,
    Long categoryId,
    @NotNull @PositiveOrZero BigDecimal cost,
    @NotNull @PositiveOrZero BigDecimal price,
    @PositiveOrZero BigDecimal weightKg,
    String color,
    @PositiveOrZero BigDecimal dimensionX,
    @PositiveOrZero BigDecimal dimensionY,
    @PositiveOrZero BigDecimal dimensionZ,
    String size,
    Boolean active,

    @Valid InitialStockDTO stock
) {
    public record InitialStockDTO(
        @NotNull Long warehouseId,
        @NotNull @PositiveOrZero Integer quantity,
        @NotNull @PositiveOrZero Integer reservedQuantity,
        @NotNull @PositiveOrZero Integer minQuantity,
        @PositiveOrZero Integer maxQuantity
    ) {}
}
