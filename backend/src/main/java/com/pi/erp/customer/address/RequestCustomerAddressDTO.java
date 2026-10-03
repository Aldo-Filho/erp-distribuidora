package com.pi.erp.customer.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequestCustomerAddressDTO(
        @NotNull
        Long customerId,
        @NotNull
        AddressType addressType,
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
