package com.pi.erp.product.catalog;

import com.pi.erp.product.ProductCharacteristic;
import com.pi.erp.product.RequestProductDTO.InitialStockDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Map;

public record RequestCatalogItemDTO(
    @NotNull @PositiveOrZero BigDecimal cost,
    @NotNull @PositiveOrZero BigDecimal price,
    @PositiveOrZero BigDecimal weightKg,
    String color,
    @PositiveOrZero BigDecimal dimensionX,
    @PositiveOrZero BigDecimal dimensionY,
    @PositiveOrZero BigDecimal dimensionZ,
    String size,
    Boolean active,
    Map<ProductCharacteristic, String> attributes,
    @Valid InitialStockDTO stock
) {}
