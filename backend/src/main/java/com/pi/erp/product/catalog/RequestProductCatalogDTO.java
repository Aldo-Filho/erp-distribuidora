package com.pi.erp.product.catalog;

import com.pi.erp.product.ProductCharacteristic;
import com.pi.erp.product.ProductUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

public record RequestProductCatalogDTO(
    @NotBlank String name,
    @NotNull Long brandId,
    Long categoryId,
    @NotNull ProductUnit unit,
    @NotNull ProductCatalogType type,
    Set<ProductCharacteristic> characteristics,
    Boolean active,
    @Valid List<RequestCatalogItemDTO> items
) {}
