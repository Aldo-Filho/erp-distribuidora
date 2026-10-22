package com.pi.erp.supplier.category;

import jakarta.validation.constraints.NotNull;

public record RequestSupplierCategoryDTO(
    @NotNull Long categoryId
) {}
