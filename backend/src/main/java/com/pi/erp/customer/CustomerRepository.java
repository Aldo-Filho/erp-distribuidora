package com.pi.erp.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository
    extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer>
{
    @Query(value = "SELECT EXISTS (SELECT 1 FROM customers WHERE upper(regexp_replace(tax_id, '[^0-9A-Za-z]', '', 'g')) = :taxId)", nativeQuery = true)
    boolean existsByTaxId(@Param("taxId") String taxId);
    @Query(value = "SELECT EXISTS (SELECT 1 FROM customers WHERE upper(regexp_replace(tax_id, '[^0-9A-Za-z]', '', 'g')) = :taxId AND customer_id <> :id)", nativeQuery = true)
    boolean existsByTaxIdAndIdNot(
        @Param("taxId") String taxId,
        @Param("id") Long id
    );
}
