package com.pi.erp.customer;

public record CustomerFilter(
        PersonType personType,
        String taxId,
        String legalName,
        String tradeName,
        String municipalReg,
        String stateReg,
        String email,
        String phone,
        String whatsapp,
        Long priceTableId,
        Boolean active
) {
}
