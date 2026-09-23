package com.pi.erp.customer.address;

public record PatchCustomerAddressDTO(
        Long customerId,
        AddressType addressType,
        String state,
        String city,
        String street,
        String neighborhood,
        String number,
        String complement,
        String zipCode
) {
}
