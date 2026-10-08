package com.pi.erp.supplier.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierAddressRepository
    extends JpaRepository<SupplierAddress, Long>, JpaSpecificationExecutor<SupplierAddress>
{
    boolean existsByZipCodeAndNumberAndComplementIgnoreCase(
        String zipCode,
        String number,
        String complement
    );

    boolean existsByZipCodeAndNumberAndComplementIgnoreCaseAndIdNot(
        String zipCode,
        String number,
        String complement,
        Long id
    );
}
