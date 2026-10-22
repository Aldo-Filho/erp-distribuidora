package com.pi.erp.customer.address;

public record PatchCustomerAddressDTO(
    AddressType addressType,
    String state,
    String city,
    String street,
    String neighborhood,
    String number,
    String complement,
    String zipCode
) {}
