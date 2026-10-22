package com.pi.erp.supplier;

import com.pi.erp.supplier.address.AddressType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RequestSupplierDTO(
    @NotBlank String legalName,
    String tradeName,
    @NotBlank String taxId,
    @Email String email,
    String phone,
    String whatsapp,
    @PositiveOrZero Integer avgDeliveryDays,
    Boolean active,

    @Valid AddressDTO address
) {
    public record AddressDTO(
        @NotBlank String country,
        @NotBlank String state,
        @NotBlank String city,
        @NotBlank String street,
        @NotBlank String neighborhood,
        @NotBlank String number,
        String complement,
        @NotBlank String zipCode,
        @NotNull AddressType addressType
    ) {}
}
