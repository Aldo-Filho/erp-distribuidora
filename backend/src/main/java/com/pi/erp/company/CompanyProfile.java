package com.pi.erp.company;

public record CompanyProfile(
    Long id, String legalName, String tradeName, String taxId, String stateReg,
    String municipalReg, TaxRegime taxRegime, Address address, long version
) {
    public enum TaxRegime { SIMPLES_NACIONAL, LUCRO_PRESUMIDO, LUCRO_REAL }

    public record Address(
        String country, String state, String city, String street, String neighborhood,
        String number, String complement, String zipCode
    ) {}
}
