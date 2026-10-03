package com.pi.erp.product.brand;

import jakarta.validation.constraints.NotBlank;

public record RequestBrandDTO(
        @NotBlank
        String name
) {
}
