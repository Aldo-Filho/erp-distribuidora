package com.pi.erp.warehouse.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WarehouseAddressRepository extends JpaRepository<WarehouseAddress, Long>, JpaSpecificationExecutor<WarehouseAddress> {
    boolean existsByZipCodeAndNumberAndComplementIgnoreCase(String zipCode, String number, String complement);
}
