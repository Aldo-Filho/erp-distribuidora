package com.pi.erp.price;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PriceTableRepository extends JpaRepository<PriceTable, Long>, JpaSpecificationExecutor<PriceTable> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
