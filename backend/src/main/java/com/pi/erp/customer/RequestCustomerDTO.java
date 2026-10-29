package com.pi.erp.customer;

import com.pi.erp.customer.address.AddressType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RequestCustomerDTO(
    @NotNull PersonType personType,
    @NotBlank String taxId,
    @NotBlank String legalName,
    String tradeName,
    LocalDate birthDate,
    String municipalReg,
    String stateReg,
    @Email String email,
    @NotBlank String phone,
    String whatsapp,
    Boolean active,
    Long priceTableId,

    @Valid AddressDTO address
) {
    public record AddressDTO(
        @NotNull AddressType addressType,
        @NotBlank String state,
        @NotBlank String city,
        @NotBlank String street,
        @NotBlank String neighborhood,
        @NotBlank String number,
        String complement,
        @NotBlank String zipCode
    ) {}
}
