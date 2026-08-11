package com.pi.erp.product.category;

import jakarta.validation.constraints.NotBlank;

public record RequestCategoryDTO(
        @NotBlank
        String name
) {
}
