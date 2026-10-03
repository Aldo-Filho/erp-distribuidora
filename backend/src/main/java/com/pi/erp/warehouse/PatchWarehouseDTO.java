package com.pi.erp.warehouse;

import jakarta.validation.constraints.NotBlank;

public record PatchWarehouseDTO(
        @NotBlank
        String description
) {
}
