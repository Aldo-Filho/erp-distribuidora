package com.pi.erp.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Map;

public record RequestProductDTO(
    String name,
    Long brandId,
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

    Long catalogId,
    ProductUnit unit,
    Map<ProductCharacteristic, String> attributes,
    @Valid InitialStockDTO stock
) {
    public record InitialStockDTO(
        @NotNull Long warehouseId,
        @NotNull @PositiveOrZero BigDecimal quantity,
        @NotNull @PositiveOrZero BigDecimal reservedQuantity,
        @NotNull @PositiveOrZero BigDecimal minQuantity,
        @PositiveOrZero BigDecimal maxQuantity
    ) {}
}
