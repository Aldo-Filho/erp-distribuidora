package com.pi.erp.supplier.brand;

import jakarta.validation.constraints.NotNull;

public record RequestSupplierBrandDTO(
    @NotNull Long brandId
) {}
