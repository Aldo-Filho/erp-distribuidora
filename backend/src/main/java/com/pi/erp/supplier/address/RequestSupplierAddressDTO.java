package com.pi.erp.supplier.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestSupplierAddressDTO(
    @NotNull Long supplierId,
    @NotBlank String country,
    @NotBlank String state,
    @NotBlank String city,
    @NotBlank String street,
    @NotBlank String neighborhood,
    @NotBlank String number,
    @NotBlank String complement,
    @NotBlank String zipCode,
    @NotNull AddressType addressType
) {}
