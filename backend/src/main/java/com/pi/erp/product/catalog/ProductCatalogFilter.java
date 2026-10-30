package com.pi.erp.product.catalog;

public record ProductCatalogFilter(
    String name,
    String sku,
    Long brandId,
    Long categoryId,
    Boolean active,
    String query
) {}
