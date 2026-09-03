package com.pi.erp.warehouse.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestWarehouseAddressDTO(
        @NotNull
        Long warehouseId,
        @NotBlank
        String state,
        @NotBlank
        String city,
        @NotBlank
        String street,
        @NotBlank
        String neighborhood,
        @NotBlank
        String number,
        @NotBlank
        String complement,
        @NotBlank
        String zipCode
) {
}