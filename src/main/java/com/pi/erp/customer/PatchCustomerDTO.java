package com.pi.erp.customer;

import java.time.LocalDate;

public record PatchCustomerDTO(
        PersonType personType,
        String taxId,
        String legalName,
        String tradeName,
        LocalDate birthDate,
        String municipalReg,
        String stateReg,
        String email,
        String phone,
        String whatsapp,
        Boolean active,
        Long priceTableId
) {
}
