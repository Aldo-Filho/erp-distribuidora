package com.pi.erp.product.brand;

import jakarta.validation.constraints.NotBlank;

public record RequestBrandDTO(
        Long id,

        @NotBlank
        String name
) {
}
