package com.pi.erp.supplier.brand;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierBrandRepository
    extends JpaRepository<SupplierBrand, SupplierBrandId>, JpaSpecificationExecutor<SupplierBrand>
{
    Page<SupplierBrand> findAllBySupplierId(Long supplierId, Pageable pageable);

    List<SupplierBrand> findAllBySupplierId(Long supplierId);

    void deleteAllBySupplierId(Long supplierId);
}
