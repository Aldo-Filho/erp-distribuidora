package com.pi.erp.product;

public record ProductFilter(
        String name,
        String sku,
        Long brandId,
        Long categoryId,
        Boolean active
) {}