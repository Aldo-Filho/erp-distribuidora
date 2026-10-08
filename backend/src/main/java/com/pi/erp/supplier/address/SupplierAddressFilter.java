package com.pi.erp.supplier.address;

public record SupplierAddressFilter(
    Long supplierId,
    AddressType addressType,
    String country,
    String state,
    String city,
    String street,
    String neighborhood,
    String number,
    String complement,
    String zipCode
) {}
