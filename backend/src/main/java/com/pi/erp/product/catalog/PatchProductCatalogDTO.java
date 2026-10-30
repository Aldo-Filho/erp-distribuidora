package com.pi.erp.product.catalog;

import com.pi.erp.product.ProductCharacteristic;
import com.pi.erp.product.ProductUnit;
import java.util.Set;

public record PatchProductCatalogDTO(
    String name,
    Long brandId,
    Long categoryId,
    ProductUnit unit,
    Set<ProductCharacteristic> characteristics,
    Boolean active,
    Boolean removeCategory
) {}
