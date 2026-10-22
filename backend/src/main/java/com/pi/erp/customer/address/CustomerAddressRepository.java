package com.pi.erp.customer.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerAddressRepository
    extends JpaRepository<CustomerAddress, Long>, JpaSpecificationExecutor<CustomerAddress>
{
    boolean existsByCustomerIdAndAddressType(
        Long customerId,
        AddressType addressType
    );
    boolean existsByCustomerIdAndAddressTypeAndIdNot(
        Long customerId,
        AddressType addressType,
        Long id
    );
}
