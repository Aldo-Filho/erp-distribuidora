package com.pi.erp.customer.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long>, JpaSpecificationExecutor<CustomerAddress> {
}
