package com.pi.erp.warehouse;

import jakarta.validation.constraints.NotBlank;

public record RequestWarehouseDTO(
        @NotBlank
        String name,
        String description
) {
}
