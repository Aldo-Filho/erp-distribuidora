package com.pi.erp.supplier;

import com.pi.erp.supplier.address.AddressType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestSupplierDTO(
    @NotBlank String legalName,
    @NotBlank String tradeName,
    @NotBlank String taxId,
    @NotBlank String email,
    @NotBlank String phone,
    String whatsapp,

    @Valid AddressDTO address
) {
    public record AddressDTO(
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
}
