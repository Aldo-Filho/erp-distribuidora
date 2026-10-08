package com.pi.erp.supplier.address;

public record PatchSupplierAddressDTO(
    String country,
    String state,
    String city,
    String street,
    String neighborhood,
    String number,
    String complement,
    String zipCode,
    AddressType addressType
) {}
