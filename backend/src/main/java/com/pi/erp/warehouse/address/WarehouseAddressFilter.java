package com.pi.erp.warehouse.address;

public record WarehouseAddressFilter(
        Long warehouseId,
        String state,
        String city,
        String street,
        String neighborhood,
        String number,
        String complement,
        String zipCode
) {
}
