package com.pi.erp.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RequestCustomerDTO(
        @NotNull
        PersonType personType,
        @NotBlank
        String taxId,
        @NotBlank
        String legalName,
        @NotBlank
        String tradeName,
        LocalDate birthDate,
        @NotBlank
        String municipalReg,
        @NotBlank
        String stateReg,
        @NotBlank
        String email,
        @NotBlank
        String phone,
        @NotBlank
        String whatsapp,
        Boolean active,
        Long priceTableId
) {
}
